/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.meta.loader;

import com.axelor.auth.AuditableRunner;
import com.axelor.auth.AuthService;
import com.axelor.auth.db.AuditableModel;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.GroupRepository;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.common.StringUtils;
import com.axelor.db.ParallelTransactionExecutor;
import com.axelor.db.internal.DBHelper;
import com.axelor.db.tenants.TenantResolver;
import com.axelor.event.Event;
import com.axelor.event.NamedLiteral;
import com.axelor.events.ModuleChanged;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.repo.MetaModuleRepository;
import com.google.common.collect.ImmutableList;
import com.google.inject.persist.Transactional;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleManager {

  private static final Logger log = LoggerFactory.getLogger(ModuleManager.class);

  private static final Resolver resolver = new Resolver();

  private boolean loadData = true;

  private final AuthService authService;

  private final MetaModuleRepository modules;

  private final ViewLoader viewLoader;

  private final DataLoader dataLoader;

  private final DemoLoader demoLoader;

  private final List<AbstractParallelLoader> metaLoaders;

  @Inject private Event<ModuleChanged> moduleChangedEvent;

  private static final Set<String> SKIP = new HashSet<>();

  private static long lastRestored;
  private final Set<Path> pathsToRestore = new HashSet<>();

  private static final AtomicBoolean BUSY = new AtomicBoolean(false);

  static {
    SKIP.add("axelor-common");
    SKIP.add("axelor-cglib");
    SKIP.add("axelor-test");
  }

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
      createUsers();
      resolve(update);
      Beans.get(AuditableRunner.class)
          .run(
              () -> {
                final List<Module> installed = new ArrayList<>();

                // install modules
                resolver.all().stream()
                    .peek(m -> log.info("Loading package {}...", m.getName()))
                    .filter(m -> m.isPending())
                    .forEach(
                        m -> {
                          if (installOne(m.getName(), update, withDemo)) {
                            installed.add(m);
                          }
                        });

                // second iteration ensures proper view sequence
                installed.forEach(module -> viewLoader.doLast(module, update));
              });
    } finally {
      encryptPasswords();
      doCleanUp();
    }
  }

  public void updateAll(boolean withDemo) {
    update(withDemo);
  }

  public void update(boolean withDemo, String... moduleNames) {
    final List<String> names = new ArrayList<>();
    if (moduleNames != null) {
      Collections.addAll(names, moduleNames);
    }

    try {
      this.createUsers();
      this.resolve(true);
      if (names.isEmpty()) {
        resolver.all().stream()
            .filter(Module::isInstalled)
            .map(Module::getName)
            .forEach(names::add);
      }
      resolver.all().stream()
          .filter(m -> names.contains(m.getName()))
          .forEach(m -> installOne(m.getName(), true, withDemo));
      resolver.all().stream()
          .filter(m -> names.contains(m.getName()))
          .forEach(m -> viewLoader.doLast(m, true));
    } finally {
      this.doCleanUp();
    }
  }

  public void update(Set<String> moduleNames, Set<Path> paths) {
    final long startTime = System.currentTimeMillis();

    final List<Module> moduleList =
        resolver.all().stream()
            .filter(m -> moduleNames.contains(m.getName()))
            .collect(Collectors.toList());

    try {
      pathsToRestore.addAll(paths);
      moduleList.forEach(m -> installOne(m.getName(), true, false));
      moduleList.forEach(m -> viewLoader.doLast(m, true));
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
      updateAll(false);
    } finally {
      BUSY.set(false);
      loadData = true;
    }
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
    return resolver.names();
  }

  static List<Module> getAll() {
    return resolver.all();
  }

  static Module getModule(String name) {
    return resolver.get(name);
  }

  public void install(String moduleName, boolean update, boolean withDemo) {
    try {
      resolver.resolve(moduleName).stream()
          .map(Module::getName)
          .forEach(name -> installOne(name, update, withDemo));
      resolver.resolve(moduleName).stream().forEach(m -> viewLoader.doLast(m, update));
    } finally {
      this.doCleanUp();
    }
  }

  private void doCleanUp() {
    doCleanUp(0);
  }

  private void doCleanUp(long time) {
    AbstractLoader.doCleanUp();
    updateLastRestored(time);
  }

  private boolean installOne(String moduleName, boolean update, boolean withDemo) {
    final Module module = resolver.get(moduleName);
    final MetaModule metaModule = modules.findByName(moduleName);

    if (metaModule == null || SKIP.contains(moduleName) || !module.isPending()) {
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

    moduleChangedEvent
        .select(NamedLiteral.of(moduleName))
        .fire(new ModuleChanged(moduleName, module.isInstalled()));

    return true;
  }

  private void installMeta(Module module, boolean update) {
    final String tenantId = TenantResolver.currentTenantIdentifier();
    final String tenantHost = TenantResolver.currentTenantHost();
    final ParallelTransactionExecutor transactionExecutor =
        new ParallelTransactionExecutor(tenantId, tenantHost);
    metaLoaders.forEach(
        metaLoader ->
            metaLoader.feedTransactionExecutor(
                transactionExecutor, module, update, pathsToRestore));
    transactionExecutor.run();
  }

  public static boolean isInstalled(String module) {
    final Module mod = resolver.get(module);
    return mod != null && mod.isInstalled();
  }

  private static Set<String> getInstalledModules() {
    final Set<String> all = new HashSet<String>();
    try (final Connection connection = DBHelper.getConnection()) {
      try (final Statement statement = connection.createStatement()) {
        try (final ResultSet rs =
            statement.executeQuery("select name from meta_module where installed = true")) {
          while (rs.next()) {
            all.add(rs.getString("name"));
          }
        }
      }
    } catch (Exception e) {
    }

    return all;
  }

  /** Find all modules which are installed or non-removable (default candidates). */
  public static List<String> findInstalled() {
    final Resolver resolver = new Resolver();
    final Set<String> installed = getInstalledModules();
    final List<String> found = new ArrayList<>();

    for (final Properties properties : MetaScanner.findModuleProperties()) {
      final String name = properties.getProperty("name");
      if (SKIP.contains(name)) {
        continue;
      }

      final String[] depends = properties.getProperty("depends", "").trim().split("\\s*,\\s*");
      final boolean application = "true".equals(properties.getProperty("application"));

      final Module module = resolver.add(name, depends);
      module.setApplication(application);
    }

    for (Module module : resolver.all()) {
      String name = module.getName();
      if (SKIP.contains(name)) continue;
      if (installed.contains(name)) {
        module.setInstalled(true);
      }
      if (module.isInstalled()) {
        found.add(name);
      }
    }

    return found;
  }

  @Transactional
  void resolve(boolean update) {
    final Map<MetaModule, String[]> dependencies = new HashMap<>();

    for (final Properties properties : MetaScanner.findModuleProperties()) {
      final String name = properties.getProperty("name");
      if (SKIP.contains(name)) {
        continue;
      }

      final String[] depends = properties.getProperty("depends", "").trim().split("\\s*,\\s*");
      final String title = properties.getProperty("title");
      final String description = properties.getProperty("description");
      final String version = properties.getProperty("version");
      final boolean application = "true".equals(properties.getProperty("application"));

      final Module module = resolver.add(name, depends);
      MetaModule stored = modules.findByName(name);

      if (stored == null) {
        stored = new MetaModule();
        stored.setName(name);
      } else {
        module.setInstalled(true);
      }

      module.setVersion(version);
      module.setApplication(application);

      if (stored.getId() == null || update) {
        stored.setTitle(title);
        stored.setDescription(description);
        stored.setModuleVersion(version);
        stored.setApplication(application);
        stored = modules.save(stored);
        dependencies.put(stored, depends);
        module.setPending(true);
      }
    }

    // resolve dependencies
    for (MetaModule stored : dependencies.keySet()) {
      final Set<MetaModule> depends = new HashSet<>();
      for (String name : dependencies.get(stored)) {
        if (StringUtils.isBlank(name)) continue;
        final MetaModule depending = modules.findByName(name);
        if (depending == null) {
          throw new RuntimeException(
              "No such depmodule found: " + name + ", required by: " + stored.getName());
        }
        depends.add(depending);
      }
      stored.clearDepends();
      stored.setDepends(depends);
    }
  }

  @Transactional
  void createUsers() {

    final UserRepository users = Beans.get(UserRepository.class);
    final GroupRepository groups = Beans.get(GroupRepository.class);

    if (users.all().count() != 0) {
      // encrypt plain passwords
      for (User user :
          users.all().filter("self.password not like :shiro").bind("shiro", "$shiro1$%").fetch()) {
        authService.encrypt(user);
      }
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

  @Transactional
  public void encryptPasswords() {
    final UserRepository users = Beans.get(UserRepository.class);

    if (users.all().count() != 0) {
      // encrypt plain passwords
      for (User user :
          users.all().filter("self.password not like :shiro").bind("shiro", "$shiro1$%").fetch()) {
        authService.encrypt(user);
      }
    }
  }
}
