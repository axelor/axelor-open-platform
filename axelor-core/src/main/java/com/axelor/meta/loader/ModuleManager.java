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
import com.axelor.db.ParallelTransactionExecutor;
import com.axelor.db.tenants.TenantResolver;
import com.axelor.event.Event;
import com.axelor.event.NamedLiteral;
import com.axelor.events.ModuleChanged;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.repo.MetaModuleRepository;
import com.google.common.collect.ImmutableList;
import com.google.inject.persist.Transactional;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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

  private final Event<ModuleChanged> moduleChangedEvent;

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
      DemoLoader demoLoader,
      Event<ModuleChanged> moduleChangedEvent) {
    this.authService = authService;
    this.modules = modules;
    this.viewLoader = viewLoader;
    this.dataLoader = dataLoader;
    this.demoLoader = demoLoader;
    this.moduleChangedEvent = moduleChangedEvent;
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
                RESOLVER.all().stream()
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

  public void update(boolean withDemo, String... moduleNames) {
    final List<String> names = new ArrayList<>();
    if (moduleNames != null) {
      Collections.addAll(names, moduleNames);
    }

    try {
      createUsers();
      resolve(true);
      if (names.isEmpty()) {
        RESOLVER.all().stream()
            .filter(Module::isInstalled)
            .map(Module::getName)
            .forEach(names::add);
      }
      Beans.get(AuditableRunner.class)
          .run(
              () -> {
                RESOLVER.all().stream()
                    .filter(m -> names.contains(m.getName()))
                    .forEach(m -> installOne(m.getName(), true, withDemo));
                RESOLVER.all().stream()
                    .filter(m -> names.contains(m.getName()))
                    .forEach(m -> viewLoader.doLast(m, true));
              });
    } finally {
      this.doCleanUp();
    }
  }

  public void update(Set<String> moduleNames, Set<Path> paths) {
    final long startTime = System.currentTimeMillis();

    final List<Module> moduleList =
        RESOLVER.all().stream()
            .filter(m -> moduleNames.contains(m.getName()))
            .collect(Collectors.toList());

    try {
      pathsToRestore.addAll(paths);
      Beans.get(AuditableRunner.class)
          .run(
              () -> {
                moduleList.forEach(m -> m.setPending(true));
                moduleList.forEach(m -> installOne(m.getName(), true, false));
                moduleList.forEach(m -> viewLoader.doLast(m, true));
              });
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

    if (metaModule == null || !module.isPending()) {
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
