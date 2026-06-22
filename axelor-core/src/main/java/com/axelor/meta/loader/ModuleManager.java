/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.loader;

import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.AuditableModel;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.GroupRepository;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.cache.DistributedFactory;
import com.axelor.common.ObjectUtils;
import com.axelor.concurrent.ContextAware;
import com.axelor.db.JPA;
import com.axelor.db.ParallelTransactionExecutor;
import com.axelor.i18n.I18n;
import com.axelor.i18n.I18nBundle;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.repo.MetaModuleRepository;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleManager {

  private static final Logger log = LoggerFactory.getLogger(ModuleManager.class);

  private static final ModuleResolver RESOLVER = ModuleResolver.scan();

  private final AuthService authService;

  private final MetaModuleRepository modules;

  private final ViewLoader viewLoader;

  private final DataLoader dataLoader;

  private final DemoLoader demoLoader;

  private final List<AbstractParallelLoader> metaLoaders;

  private static long lastRestored;
  private final Set<Path> pathsToRestore = new HashSet<>();

  @Inject
  public ModuleManager(
      AuthService authService,
      MetaModuleRepository modules,
      ViewLoader viewLoader,
      ModelLoader modelLoader,
      I18nLoader i18nLoader,
      DataLoader dataLoader,
      DemoLoader demoLoader) {
    this.authService = authService;
    this.modules = modules;
    this.viewLoader = viewLoader;
    this.dataLoader = dataLoader;
    this.demoLoader = demoLoader;
    metaLoaders = List.of(modelLoader, viewLoader, i18nLoader);
  }

  /**
   * Initialize the application by loading the pending modules.
   *
   * <p>Called on application start: only pending (i.e. new, not-yet-installed) modules are loaded,
   * so already-installed modules are left untouched.
   *
   * @param withDemo whether to load demo data for the newly installed modules
   */
  public void initialize(final boolean withDemo) {
    var lock = DistributedFactory.getLockIfDistributed("initialize");
    lock.lock();
    try {
      load(
          false,
          withDemo,
          () ->
              RESOLVER.all().stream()
                  .peek(m -> log.info("Loading package {}...", m.getName()))
                  .filter(Module::isPending)
                  .collect(Collectors.toList()));
    } finally {
      lock.unlock();
    }
  }

  // When using distributed cache, it may contain incompatible data.
  // Need to clear it before loading new modules.
  private void evictAllCacheRegions() {
    try {
      var sessionFactory = JPA.em().unwrap(Session.class).getSessionFactory();
      sessionFactory.getCache().evictAllRegions();
    } catch (Exception e) {
      log.error("Failed to evict regions", e);
    }
  }

  /** Clears all meta caches. */
  private void clearMetaCaches() {
    try {
      MetaStore.clear();
      I18nBundle.invalidate();
    } catch (Exception e) {
      log.error("Failed to clear meta caches", e);
    }
  }

  /**
   * Update the modules of the database.
   *
   * <p>When no module name is given, all modules are updated and any new (not-yet-installed) module
   * is installed; otherwise only the given modules are updated, if they exist.
   *
   * @param withDemo whether to load demo data for the newly installed modules
   * @param moduleNames the modules to update; if empty, all modules are updated
   */
  public void update(boolean withDemo, String... moduleNames) {
    load(
        true,
        withDemo,
        () ->
            ObjectUtils.isEmpty(moduleNames)
                ? new ArrayList<>(RESOLVER.all())
                : RESOLVER.all().stream()
                    .filter(m -> Arrays.asList(moduleNames).contains(m.getName()))
                    .collect(Collectors.toList()));
  }

  /**
   * Reload the given paths of the given modules.
   *
   * <p>Only the resources matching {@code paths} are processed, restricted to the given modules.
   * Used to hot-reload changed files, e.g. by the view watcher.
   *
   * @param moduleNames the names of the modules to reload
   * @param paths the resource paths to reload
   */
  public void update(Set<String> moduleNames, Set<Path> paths) {
    final long startTime = System.currentTimeMillis();

    try {
      pathsToRestore.addAll(paths);
      process(
          false, RESOLVER.all().stream().filter(m -> moduleNames.contains(m.getName())).toList());
    } finally {
      pathsToRestore.clear();
      updateLastRestored(startTime);
    }
  }

  public void restoreMeta() {
    var busy = DistributedFactory.getAtomicLong("busy");
    try {
      if (!busy.compareAndSet(0, 1)) {
        throw new IllegalStateException(
            I18n.get(
                "A views restoring is already in progress. Please wait until it ends and try again."));
      }
      // Restore meta of installed modules only.
      process(false, RESOLVER.all().stream().filter(Module::isInstalled).toList());
    } finally {
      busy.set(0);
    }
  }

  /**
   * Create the default data, resolve the modules, then load the selected ones.
   *
   * <p>The selection is deferred until after resolve so it can rely on the up-to-date
   * install/pending state.
   *
   * @param forceResolve whether to refresh module metadata and re-mark all modules as pending
   * @param withDemo whether to load demo data for the newly installed modules
   * @param moduleSelector supplies the modules to load, evaluated after resolve
   */
  private void load(boolean forceResolve, boolean withDemo, Supplier<List<Module>> moduleSelector) {
    createDefault();
    resolve(forceResolve);
    process(withDemo, moduleSelector.get());
  }

  /**
   * Select the modules, evict the cache when any of them is new, install them, then finally clean
   * up, including cache clearing.
   *
   * @param withDemo whether to load demo data for the newly installed modules
   * @param moduleList the modules to load
   */
  private void process(boolean withDemo, List<Module> moduleList) {
    try {
      if (moduleList.stream().anyMatch(m -> !m.isInstalled())) {
        evictAllCacheRegions();
      }
      installModules(moduleList, withDemo);
    } finally {
      doCleanUp();
      if (ObjectUtils.notEmpty(moduleList)) {
        clearMetaCaches();
      }
    }
  }

  private void installModules(List<Module> moduleList, boolean withDemo) {
    if (ObjectUtils.isEmpty(moduleList)) {
      return;
    }

    viewLoader.initialize();
    try {
      ContextAware.of()
          .withTransaction(false)
          .withUser(AuthUtils.getUser("admin"))
          .build(
              () -> {
                moduleList.forEach(m -> installOne(m.getName(), withDemo));
                moduleList.forEach(viewLoader::doLast);
              })
          .run();
    } finally {
      viewLoader.terminate();
    }
  }

  static long getLastRestored() {
    return lastRestored;
  }

  private static void updateLastRestored(long time) {
    lastRestored = time != 0 ? time : System.currentTimeMillis();
  }

  public static List<String> getResolution() {
    return RESOLVER.names();
  }

  static List<Module> getAll() {
    return RESOLVER.all();
  }

  static Module getModule(String name) {
    return RESOLVER.get(name);
  }

  private void doCleanUp() {
    AbstractLoader.doCleanUp();
    updateLastRestored(0);
  }

  private void installOne(String moduleName, boolean withDemo) {
    final Module module = RESOLVER.get(moduleName);
    final MetaModule metaModule = modules.findByName(moduleName);

    if (metaModule == null) {
      log.error("Trying to install an nonexistent module {}. Skipping...", moduleName);
      return;
    }

    final String message =
        module.isInstalled() ? "Updating package {}..." : "Installing package {}...";
    log.info(message, moduleName);

    // load meta
    installMeta(module);

    // load data
    if (!module.isInstalled()) {
      dataLoader.load(module);
      if (withDemo) {
        demoLoader.load(module);
      }
    }

    // finally update install state
    module.setPending(false);
    module.setInstalled(true);
  }

  private void installMeta(Module module) {
    final ParallelTransactionExecutor transactionExecutor = new ParallelTransactionExecutor();
    metaLoaders.forEach(
        metaLoader ->
            metaLoader.feedTransactionExecutor(transactionExecutor, module, pathsToRestore));
    transactionExecutor.run();
    JPA.em().clear();
  }

  public static boolean isInstalled(String module) {
    final Module mod = RESOLVER.get(module);
    return mod != null && mod.isInstalled();
  }

  @Transactional
  void resolve(boolean update) {

    List<MetaModule> saved = new ArrayList<>();

    for (Module module : RESOLVER.all()) {
      String name = module.getName();
      String title = module.getTitle();
      String description = module.getDescription();
      String version = module.getVersion();

      MetaModule stored = modules.findByName(name);

      if (stored == null) {
        stored = new MetaModule();
        stored.setName(name);
      } else {
        module.setInstalled(true);
      }

      if (stored.getId() == null || update) {
        stored.setTitle(title);
        stored.setDescription(description);
        stored.setModuleVersion(version);
        stored.setApplication(module.isApplication());
        stored = modules.save(stored);
        module.setPending(true);
      }

      saved.add(stored);
    }

    // set dependencies
    for (MetaModule stored : saved) {
      final Set<MetaModule> depends = new HashSet<>();
      final Module module = RESOLVER.get(stored.getName());
      for (Module dep : module.getDepends()) {
        depends.add(modules.findByName(dep.getName()));
      }
      stored.clearDepends();
      stored.setDepends(depends);
    }
  }

  /**
   * Create the default user (<code>admin</code>) and groups (<code>admins</code>, <code>users
   * </code>).
   */
  @Transactional
  void createDefault() {

    final UserRepository users = Beans.get(UserRepository.class);
    final GroupRepository groups = Beans.get(GroupRepository.class);

    if (users.all().count() != 0) {
      return;
    }

    User admin = users.findByCode("admin");
    if (admin != null) {
      return;
    }

    Group adminGroup = groups.findByCode("admins");
    Group userGroup = groups.findByCode("users");

    if (adminGroup == null) {
      adminGroup = new Group("admins", "Administrators");
    }

    if (userGroup == null) {
      userGroup = new Group("users", "Users");
    }

    admin = new User("admin", "Administrator");
    admin.setPassword(authService.encrypt("admin"));
    admin.setGroup(adminGroup);

    // set createdBy property to admin
    try {
      Field createdBy = AuditableModel.class.getDeclaredField("createdBy");
      createdBy.setAccessible(true);
      createdBy.set(adminGroup, admin);
      createdBy.set(userGroup, admin);
      createdBy.set(admin, admin);
    } catch (Exception e) {
      // ignore
    }

    admin = users.save(admin);
  }
}
