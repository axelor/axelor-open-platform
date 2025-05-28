/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.meta.loader;

import com.axelor.auth.AuditableRunner;
import com.axelor.auth.AuthService;
import com.axelor.auth.db.AuditableModel;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.GroupRepository;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.common.ObjectUtils;
import com.axelor.db.JPA;
import com.axelor.db.ParallelTransactionExecutor;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.repo.MetaModuleRepository;
import com.google.common.collect.ImmutableList;
import com.google.inject.persist.Transactional;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleManager {

  private static final Logger log = LoggerFactory.getLogger(ModuleManager.class);

  private static final ModuleResolver RESOLVER = ModuleResolver.scan();

  private boolean loadData = true;

  private final AuthService authService;

  private final MetaModuleRepository modules;

  private final ViewLoader viewLoader;

  private final DataLoader dataLoader;

  private final DemoLoader demoLoader;

  private final List<AbstractParallelLoader> metaLoaders;

  private static long lastRestored;
  private final Set<Path> pathsToRestore = new HashSet<>();

  private static final AtomicBoolean BUSY = new AtomicBoolean(false);

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
    metaLoaders = ImmutableList.of(modelLoader, viewLoader, i18nLoader);
  }

  public void initialize(final boolean update, final boolean withDemo) {
    try {
      createDefault();
      resolve(update);
      final List<Module> moduleList =
          RESOLVER.all().stream()
              .peek(m -> log.info("Loading package {}...", m.getName()))
              .filter(Module::isPending)
              .collect(Collectors.toList());
      loadModules(moduleList, update, withDemo);
    } finally {
      doCleanUp();
    }
  }

  public void update(boolean withDemo, String... moduleNames) {
    final List<Module> moduleList = new ArrayList<>();

    try {
      resolve(true);
      if (ObjectUtils.isEmpty(moduleNames)) {
        RESOLVER.all().stream().filter(Module::isInstalled).forEach(moduleList::add);
      } else {
        RESOLVER.all().stream()
            .filter(m -> Arrays.asList(moduleNames).contains(m.getName()))
            .forEach(moduleList::add);
      }
      loadModules(moduleList, true, withDemo);
    } finally {
      this.doCleanUp();
    }
  }

  public void update(Set<String> moduleNames, Set<Path> paths) {
    final long startTime = System.currentTimeMillis();

    final List<Module> moduleList =
        RESOLVER.all().stream()
            .filter(m -> moduleNames.contains(m.getName()))
            .peek(m -> m.setPending(true))
            .collect(Collectors.toList());

    try {
      pathsToRestore.addAll(paths);
      loadModules(moduleList, true, false);
    } finally {
      pathsToRestore.clear();
      doCleanUp(startTime);
    }
  }

  public void restoreMeta() {
    try {
      if (!BUSY.compareAndSet(false, true)) {
        throw new IllegalStateException(
            I18n.get(
                "A views restoring is already in progress. Please wait until it ends and try again."));
      }
      loadData = false;
      update(false);
    } finally {
      BUSY.set(false);
      loadData = true;
    }
  }

  private void loadModules(List<Module> moduleList, boolean update, boolean withDemo) {
    Beans.get(AuditableRunner.class)
        .run(
            () -> {
              moduleList.forEach(m -> installOne(m.getName(), update, withDemo));
              moduleList.forEach(m -> viewLoader.doLast(m, update));
            });
    viewLoader.terminate(update);
  }

  public boolean isLoadData() {
    return loadData;
  }

  public void setLoadData(boolean loadData) {
    this.loadData = loadData;
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
    doCleanUp(0);
  }

  private void doCleanUp(long time) {
    AbstractLoader.doCleanUp();
    updateLastRestored(time);
  }

  private boolean installOne(String moduleName, boolean update, boolean withDemo) {
    final Module module = RESOLVER.get(moduleName);
    final MetaModule metaModule = modules.findByName(moduleName);

    if (metaModule == null) {
      log.error("Trying to install an nonexistent module {}. Skipping...", moduleName);
      return false;
    }

    final String message =
        module.isInstalled() ? "Updating package {}..." : "Installing package {}...";
    log.info(message, moduleName);

    // load meta
    installMeta(module, update);

    // load data (runs in it's own transaction)
    if (loadData) {
      dataLoader.load(module, update);
      if (withDemo) {
        demoLoader.load(module, update);
      }
    }

    // finally update install state
    module.setPending(false);
    module.setInstalled(true);

    return true;
  }

  private void installMeta(Module module, boolean update) {
    final ParallelTransactionExecutor transactionExecutor = new ParallelTransactionExecutor();
    metaLoaders.forEach(
        metaLoader ->
            metaLoader.feedTransactionExecutor(
                transactionExecutor, module, update, pathsToRestore));
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

      module.setVersion(version);

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
    }

    admin = users.save(admin);
  }
}
