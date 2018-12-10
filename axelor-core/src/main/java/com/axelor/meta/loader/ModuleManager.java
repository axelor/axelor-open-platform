/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
import com.axelor.db.internal.DBHelper;
import com.axelor.event.Event;
import com.axelor.event.NamedLiteral;
import com.axelor.events.ModuleChanged;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.repo.MetaActionMenuRepository;
import com.axelor.meta.db.repo.MetaActionRepository;
import com.axelor.meta.db.repo.MetaMenuRepository;
import com.axelor.meta.db.repo.MetaModuleRepository;
import com.axelor.meta.db.repo.MetaSelectRepository;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.google.inject.persist.Transactional;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleManager {

  private static final Logger log = LoggerFactory.getLogger(ModuleManager.class);

  private static final Resolver resolver = new Resolver();

  private boolean loadData = true;

  @Inject private AuthService authService;

  @Inject private MetaModuleRepository modules;

  @Inject private ViewLoader viewLoader;

  @Inject private ModelLoader modelLoader;

  @Inject private I18nLoader i18nLoader;

  @Inject private DataLoader dataLoader;

  @Inject private DemoLoader demoLoader;

  @Inject private Event<ModuleChanged> moduleChangedEvent;

  private static final Set<String> SKIP = new HashSet<>();

  static {
    SKIP.add("axelor-common");
    SKIP.add("axelor-cglib");
    SKIP.add("axelor-test");
  }

  public ModuleManager() {}

  public void initialize(final boolean update, final boolean withDemo) {
    try {
      createUsers();
      resolve(true);
      Beans.get(AuditableRunner.class)
          .run(
              () -> {
                // install modules
                resolver
                    .all()
                    .stream()
                    .filter(m -> !m.isRemovable() || m.isInstalled())
                    .peek(m -> log.info("Loading package " + m.getName() + "..."))
                    .filter(m -> !m.isRemovable() || m.isPending())
                    .forEach(m -> install(m.getName(), update, withDemo, false));

                // second iteration ensures proper view sequence
                resolver
                    .all()
                    .stream()
                    .filter(Module::isInstalled)
                    .forEach(m -> viewLoader.doLast(m, update));

                // uninstall pending modules
                resolver
                    .all()
                    .stream()
                    .filter(Module::isRemovable)
                    .filter(Module::isPending)
                    .filter(m -> !m.isInstalled())
                    .map(Module::getName)
                    .forEach(this::uninstall);
              });
    } finally {
      this.encryptPasswords();
      this.doCleanUp();
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
        resolver
            .all()
            .stream()
            .filter(Module::isInstalled)
            .map(Module::getName)
            .forEach(names::add);
      }
      resolver
          .all()
          .stream()
          .filter(m -> names.contains(m.getName()))
          .forEach(m -> install(m, true, withDemo));
      resolver
          .all()
          .stream()
          .filter(m -> names.contains(m.getName()))
          .forEach(m -> viewLoader.doLast(m, true));
    } finally {
      this.doCleanUp();
    }
  }

  public void restoreMeta() {
    try {
      loadData = false;
      updateAll(false);
    } finally {
      loadData = true;
    }
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
      resolver
          .resolve(moduleName)
          .stream()
          .map(Module::getName)
          .forEach(name -> install(name, update, withDemo, true));
      resolver.resolve(moduleName).stream().forEach(m -> viewLoader.doLast(m, update));
    } finally {
      this.doCleanUp();
    }

    moduleChangedEvent
        .select(NamedLiteral.of(moduleName))
        .fire(new ModuleChanged(moduleName, true));
  }

  @Transactional
  public void uninstall(String moduleName) {
    log.info("Removing package {}...", moduleName);

    final MetaModule entity = modules.findByName(moduleName);

    Beans.get(MetaViewRepository.class).findByModule(moduleName).remove();
    Beans.get(MetaSelectRepository.class).findByModule(moduleName).remove();
    Beans.get(MetaMenuRepository.class).findByModule(moduleName).remove();
    Beans.get(MetaActionRepository.class).findByModule(moduleName).remove();
    Beans.get(MetaActionMenuRepository.class).findByModule(moduleName).remove();

    entity.setInstalled(false);
    entity.setPending(false);

    modules.save(entity);

    resolver.get(moduleName).setInstalled(false);
    resolver.get(moduleName).setPending(false);

    moduleChangedEvent
        .select(NamedLiteral.of(moduleName))
        .fire(new ModuleChanged(moduleName, false));
  }

  private void doCleanUp() {
    AbstractLoader.doCleanUp();
  }

  private void install(String moduleName, boolean update, boolean withDemo, boolean force) {
    final Module module = resolver.get(moduleName);
    final MetaModule metaModule = modules.findByName(moduleName);
    if (metaModule == null) {
      return;
    }
    if (!module.isInstalled() && module.isRemovable() && !force) {
      return;
    }
    if (module.isInstalled() && !(update || module.isUpgradable() || module.isPending())) {
      return;
    }
    install(module, update, withDemo);
  }

  private void install(Module module, boolean update, boolean withDemo) {
    if (SKIP.contains(module.getName())) {
      return;
    }

    String message = "Installing package ";
    if (module.isInstalled()) {
      message = "Updating package ";
    }

    log.info(message + module + "...");

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
    updateState(module);
  }

  @Transactional
  void installMeta(Module module, boolean update) {
    // load model info
    modelLoader.load(module, update);

    // load views
    viewLoader.load(module, update);

    // load i18n
    i18nLoader.load(module, update);
  }

  @Transactional
  void updateState(Module module) {
    MetaModule metaModule = modules.findByName(module.getName());
    module.setInstalled(true);
    module.setPending(false);
    module.setInstalledVersion(module.getVersion());
    metaModule.setInstalled(true);
    metaModule.setPending(false);
  }

  public static boolean isInstalled(String module) {
    final Module mod = resolver.get(module);
    return mod != null && mod.isInstalled();
  }

  private static Set<String> getInstalledModules() {
    final Set<String> all = new HashSet<String>();
    try (final Connection connection = DBHelper.getConnection();
        final Statement statement = connection.createStatement();
        final ResultSet rs =
            statement.executeQuery("select name from meta_module where installed = true")) {
      while (rs.next()) {
        all.add(rs.getString("name"));
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
      final String[] installs = properties.getProperty("installs", "").trim().split("\\s*,\\s*");
      final boolean removable = "true".equals(properties.getProperty("removable"));
      final boolean application = "true".equals(properties.getProperty("application"));

      final Module module = resolver.add(name, depends);
      module.setRemovable(removable);
      module.setApplication(application);

      // install forced modules on init
      if (installed.isEmpty()) {
        installed.addAll(Arrays.asList(installs));
      }
    }

    for (Module module : resolver.all()) {
      String name = module.getName();
      if (SKIP.contains(name)) continue;
      if (installed.contains(name)) {
        module.setInstalled(true);
      }
      if (module.isInstalled() || !module.isRemovable()) {
        found.add(name);
      }
    }

    return found;
  }

  @Transactional
  void resolve(boolean update) {
    final Set<String> forceInstall = new HashSet<>();
    final Map<MetaModule, String[]> dependencies = new HashMap<>();
    final boolean forceInit = modules.all().count() == 0;

    for (final Properties properties : MetaScanner.findModuleProperties()) {
      final String name = properties.getProperty("name");
      if (SKIP.contains(name)) {
        continue;
      }

      final String[] depends = properties.getProperty("depends", "").trim().split("\\s*,\\s*");
      final String title = properties.getProperty("title");
      final String description = properties.getProperty("description");
      final String version = properties.getProperty("version");
      final boolean removable = "true".equals(properties.getProperty("removable"));
      final boolean application = "true".equals(properties.getProperty("application"));

      if (forceInit && forceInstall.isEmpty()) {
        String[] installs = properties.getProperty("installs", "").trim().split("\\s*,\\s*");
        forceInstall.addAll(Arrays.asList(installs));
      }

      final Module module = resolver.add(name, depends);
      MetaModule stored = modules.findByName(name);
      if (stored == null) {
        stored = new MetaModule();
        stored.setName(name);
      }

      if (stored.getId() == null || update) {
        stored.setTitle(title);
        stored.setDescription(description);
        stored.setModuleVersion(version);
        stored.setRemovable(removable);
        stored.setApplication(application);
        stored = modules.save(stored);
        dependencies.put(stored, depends);
      }

      module.setVersion(version);
      module.setApplication(application);
      module.setRemovable(removable);
      module.setInstalled(stored.getInstalled() == Boolean.TRUE);
      module.setPending(stored.getPending() == Boolean.TRUE);
      module.setInstalledVersion(stored.getModuleVersion());
    }

    // resolve dependencies
    for (MetaModule stored : dependencies.keySet()) {
      final Set<MetaModule> depends = new HashSet<>();
      for (String name : dependencies.get(stored)) {
        if (StringUtils.isBlank(name)) continue;
        final MetaModule depending = modules.findByName(name);
        if (depending == null) {
          throw new RuntimeException(
              "No such depemodule found: " + name + ", required by: " + stored.getName());
        }
        depends.add(depending);
      }
      stored.clearDepends();
      stored.setDepends(depends);
    }

    for (String name : forceInstall) {
      Module module = resolver.get(name);
      MetaModule stored = modules.findByName(name);
      if (module == null || stored == null) {
        continue;
      }
      module.setPending(stored.getInstalled() != Boolean.TRUE);
      stored.setPending(stored.getInstalled() != Boolean.TRUE);
      module.setInstalled(true);
      stored.setInstalled(true);
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
