/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthService;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.db.MetaView;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.persist.Transactional;

@Singleton
public class ModuleManager {

	private static final Logger log = LoggerFactory.getLogger(ModuleManager.class);

	private static final Resolver resolver = new Resolver();

	@Inject
	private AuthService authService;

	@Inject
	private ViewLoader viewLoader;

	@Inject
	private ModelLoader modelLoader;

	@Inject
	private I18nLoader i18nLoader;

	@Inject
	private DataLoader dataLoader;

	@Inject
	private DemoLoader demoLoader;

	private static final Set<String> SKIP = Sets.newHashSet(
			"axelor-cglib",
			"axelor-test",
			"axelor-web");

	public ModuleManager() {

	}

	public void initialize(boolean update, boolean withDemo) {

		this.createUsers();
		this.resolve(true);

		log.info("modules found:");
		for (String name : resolver.names()) {
			log.info("  " + name);
		}

		for (Module module : resolver.all()) {
			if (!module.isRemovable()) {
				install(module.getName(), update, withDemo, false);
			}
		}
	}

	public void updateAll(boolean withDemo) {

		this.createUsers();
		this.resolve(true);

		for (Module module : resolver.all()) {
			install(module.getName(), true, withDemo, false);
		}
	}

	public void update(boolean withDemo, String... modules) {

		this.createUsers();
		this.resolve(true);

		List<String> names = Lists.newArrayList();
		if (modules != null) {
			names = Lists.newArrayList(modules);
		}
		if (names.isEmpty()) {
			names = resolver.names();
		}

		for (Module module : resolver.all()) {
			if (names.contains(module.getName())) {
				install(module, true, withDemo);
			}
		}
	}

	public static List<String> getResolution() {
		return resolver.names();
	}

	public static List<Module> getAll() {
		return resolver.all();
	}

	public static String getModulePath(String module) {
		try {
			return resolver.get(module).getPath();
		} catch (NullPointerException e) {
			return null;
		}
	}

	public void install(String moduleName, boolean update, boolean withDemo) {
		for (Module module: resolver.resolve(moduleName)) {
			install(module.getName(), update, withDemo, true);
		}
	}

	@Transactional
	public void uninstall(String module) {

		log.info("Uninstall module: {}", module);

		MetaModule entity = MetaModule.findByName(module);
		if (entity == null || entity.getInstalled() != Boolean.TRUE) {
			log.error("Module not installed: {}", module);
		}

		MetaView.findByModule(module).remove();
		MetaSelect.findByModule(module).remove();
		MetaMenu.findByModule(module).remove();
		MetaAction.findByModule(module).remove();
		MetaTranslation.findByModule(module).remove();

		entity.setInstalled(false);
		entity.save();

		resolver.get(module).setInstalled(false);

		log.info("Module uninstalled: {}", module);
	}

	private void install(String moduleName, boolean update, boolean withDemo, boolean force) {

		final Module module = resolver.get(moduleName);
		final MetaModule metaModule = MetaModule.findByName(moduleName);

		if (metaModule == null) {
			return;
		}
		if (!module.isInstalled() && module.isRemovable() && !force) {
			return;
		}
		if (module.isInstalled() && !(update || module.isUpgradable())) {
			return;
		}

		install(module, update, withDemo);
	}

	private void install(Module module, boolean update, boolean withDemo) {

		if (SKIP.contains(module.getName())) {
			return;
		}

		String message = "installing: {}";
		if (module.isInstalled()) {
			message = "updating: {}";
		}

		log.info(message, module);

		// load meta
		installMeta(module, update);

		// load data (runs in it's own transaction)
		dataLoader.load(module, update);
		if (withDemo) {
			demoLoader.load(module, update);
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
		MetaModule metaModule = MetaModule.findByName(module.getName());
		module.setInstalled(true);
		metaModule.setInstalled(true);
		module.setInstalledVersion(module.getVersion());
	}

	public static boolean isInstalled(String module) {
		final Module mod = resolver.get(module);
		return mod != null && mod.isInstalled();
	}

	public static List<String> findInClassPath(boolean includeRemovables) {
		final Resolver resolver = new Resolver();
		final List<String> found = Lists.newArrayList();

		for (URL file : MetaScanner.findAll("module\\.properties")) {
			Properties properties = new Properties();
			try {
				properties.load(file.openStream());
			} catch (IOException e) {
				throw Throwables.propagate(e);
			}

			String name = properties.getProperty("name");

			if (SKIP.contains(name)) {
				continue;
			}

			String[] deps = properties.getProperty("depends", "").trim().split("\\s+");
			boolean removable = "true".equals(properties.getProperty("removable"));

			Module module = resolver.add(name, deps);
			module.setRemovable(removable);
			module.setPath(file.getPath());
		}

		for (Module module : resolver.all()) {
			if (!includeRemovables && module.isRemovable()) {
				continue;
			}
			found.add(module.getName());
		}
		return found;
	}

	@Transactional
	void resolve(boolean update) {
		for (URL file : MetaScanner.findAll("module\\.properties")) {
			Properties properties = new Properties();
			try {
				properties.load(file.openStream());
			} catch (IOException e) {
				throw Throwables.propagate(e);
			}

			String name = properties.getProperty("name");

			if (SKIP.contains(name)) {
				continue;
			}

			String[] deps = properties.getProperty("depends", "").trim().split("\\s+");
			String title = properties.getProperty("title");
			String description = properties.getProperty("description");
			String version = properties.getProperty("version");
			boolean removable = "true".equals(properties.getProperty("removable"));

			Module module = resolver.add(name, deps);
			MetaModule stored = MetaModule.findByName(name);
			if (stored == null) {
				stored = new MetaModule();
				stored.setName(name);
				stored.setDepends(Joiner.on(",").join(deps));
			}

			if (stored.getId() == null || update) {
				stored.setTitle(title);
				stored.setDescription(description);
				stored.setModuleVersion(version);
				stored.setRemovable(removable);
				stored = stored.save();
			}

			module.setPath(file.getPath());
			module.setVersion(version);
			module.setRemovable(removable);
			module.setInstalled(stored.getInstalled() == Boolean.TRUE);
			module.setInstalledVersion(stored.getModuleVersion());
		}
	}

	@Transactional
	void createUsers() {

		if(User.all().count() != 0) {
			return;
		}

		User admin = User.findByCode("admin");
		if (admin != null) {
			return;
		}

		Group admins = Group.findByCode("admins");
		Group users = Group.findByCode("users");

		if (admins == null) {
			admins = new Group("admins", "Administrators");
			admins = admins.save();
		}

		if (users == null) {
			users = new Group("users", "Users");
			users = users.save();
		}

		admin = new User("admin", "Administrator");
		admin.setPassword(authService.encrypt("admin"));
		admin.setGroup(admins);
		admin = admin.save();

		User demo = new User("demo", "Demo User");
		demo.setPassword(authService.encrypt("demo"));
		demo.setGroup(users);
		demo = demo.save();
	}
}
