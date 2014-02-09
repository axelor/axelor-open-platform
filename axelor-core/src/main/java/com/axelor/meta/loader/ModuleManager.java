package com.axelor.meta.loader;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reflections.vfs.Vfs.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthService;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.db.MetaModule;
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
	
	private static final Set<String> SKIP = Sets.newHashSet(
			"axelor-cglib",
			"axelor-test",
			"axelor-web");

	public ModuleManager() {

	}

	@Transactional
	public void initialize(boolean update) {

		this.createUsers();
		this.resolve(true);

		log.info("modules found:");
		for (String name : resolver.names()) {
			log.info("  " + name);
		}

		for (Module module : resolver.all()) {
			if (!module.isRemovable()) {
				install(module.getName(), update, false);
			}
		}
	}

	/**
	 * Update installed modules.<br>
	 * <br>
	 * If module names are not given then it updates all the installed modules.
	 * 
	 * @param modules
	 *            name of modules to update
	 */
	@Transactional
	public void update(String... modules) {

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
				install(module);
			}
		}
	}

	public static List<String> getResolution() {
		return resolver.names();
	}
	
	@Transactional
	public void install(String moduleName, boolean update) {
		install(moduleName, update, true);
	}

	@Transactional
	public void uninstall(String module) {
		log.info("TODO: uninstall module: {}", module);
	}

	private void install(String moduleName, boolean update, boolean force) {

		for (Module module : resolver.resolve(moduleName)) {
			
			final MetaModule metaModule = MetaModule.findByName(moduleName);
			if (metaModule == null) {
				continue;
			}
			if (!module.isInstalled() && module.isRemovable() && !force) {
				continue;
			}
			if (module.isInstalled() && !(update || module.isUpgradable())) {
				continue;
			}

			install(module);
		}
	}
	
	private void install(Module module) {
		
		if (SKIP.contains(module.getName())) {
			return;
		}
		
		String message = "installing: {}";
		if (module.isInstalled()) {
			message = "updating: {}";
		}
		
		log.info(message, module);

		// load model info
		modelLoader.load(module);

		// load views
		viewLoader.load(module);
		
		// load i18n
		i18nLoader.load(module);

		MetaModule metaModule = MetaModule.findByName(module.getName());
		
		module.setInstalled(true);
		metaModule.setInstalled(true);
		module.setInstalledVersion(module.getVersion());
	}

	@Transactional
	private void resolve(boolean update) {
		for (File file : MetaScanner.findAll("module\\.properties")) {
			Properties properties = new Properties();
			try {
				properties.load(file.openInputStream());
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

			module.setVersion(version);
			module.setRemovable(removable);
			module.setInstalled(stored.getInstalled() == Boolean.TRUE);
			module.setInstalledVersion(stored.getModuleVersion());
		}
	}
	
	@Transactional
	private void createUsers() {
		
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
