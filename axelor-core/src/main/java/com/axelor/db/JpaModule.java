/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
package com.axelor.db;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.app.AppSettings;
import com.axelor.common.ClassUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.internal.DBHelper;
import com.google.inject.AbstractModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.JpaPersistModule;

/**
 * A Guice module to configure JPA.
 *
 * This module takes care of initializing JPA and registers an Hibernate custom
 * scanner that automatically scans all the classpath entries for Entity
 * classes.
 *
 */
public class JpaModule extends AbstractModule {

	private static Logger log = LoggerFactory.getLogger(JpaModule.class);

	private String jpaUnit;
	private boolean autoscan;
	private boolean autostart;

	private Properties properties;

	static {
		JpaScanner.exclude("com.axelor.test.db");
		JpaScanner.exclude("com.axelor.web.db");
	}

	/**
	 * Create new instance of the {@link JpaModule} with the given persistence
	 * unit name.
	 *
	 * If <i>autoscan</i> is true then a custom Hibernate scanner will be used to scan
	 * all the classpath entries for Entity classes.
	 *
	 * If <i>autostart</i> is true then the {@link PersistService} will be started
	 * automatically.
	 *
	 * @param jpaUnit
	 *            the persistence unit name
	 * @param autoscan
	 *            whether to enable autoscan
	 * @param autostart
	 *            whether to automatically start persistence service
	 */
	public JpaModule(String jpaUnit, boolean autoscan, boolean autostart) {
		this.jpaUnit = jpaUnit;
		this.autoscan = autoscan;
		this.autostart = autostart;
	}

	/**
	 * Create a new instance of the {@link JpaModule} with the given persistence
	 * unit name with <i>autoscan</i> and <i>autostart</i> enabled.
	 *
	 * @param jpaUnit
	 *            the persistence unit name
	 */
	public JpaModule(String jpaUnit) {
		this(jpaUnit, true, true);
	}

	public JpaModule scan(String pkg) {
		JpaScanner.include(pkg);
		return this;
	}

	/**
	 * Configures the JPA persistence provider with a set of properties.
	 *
	 * @param properties
	 *            A set of name value pairs that configure a JPA persistence
	 *            provider as per the specification.
	 */
	public JpaModule properties(final Properties properties) {
		this.properties = properties;
		return this;
	}

	@Override
	protected void configure() {
		log.info("Configuring JPA...");
		Properties properties = new Properties();
		if (this.properties != null) {
			properties.putAll(this.properties);
		}
		if (this.autoscan) {
			properties.put("hibernate.ejb.resource_scanner", "com.axelor.db.JpaScanner");
		}
		
		properties.put("hibernate.ejb.interceptor", "com.axelor.auth.AuditInterceptor");

		properties.put("hibernate.connection.autocommit", "false");
		properties.put("hibernate.id.new_generator_mappings", "true");
		properties.put("hibernate.ejb.naming_strategy", "org.hibernate.cfg.ImprovedNamingStrategy");
		properties.put("hibernate.connection.charSet", "UTF-8");
		properties.put("hibernate.max_fetch_depth", "3");

		properties.put("jadira.usertype.autoRegisterUserTypes", "true");
		properties.put("jadira.usertype.databaseZone", "jvm");

		if (DBHelper.isCacheEnabled()) {
			properties.put("hibernate.cache.use_second_level_cache", "true");
			properties.put("hibernate.cache.use_query_cache", "true");
			properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
			try {
				updateCacheProperties(properties);
			} catch (Exception e) {
			}
		}
		
		try {
			updatePersistenceProperties(properties);
		} catch (Exception e) {
		}
		
		install(new JpaPersistModule(jpaUnit).properties(properties));
		if (this.autostart) {
			bind(Initializer.class).asEagerSingleton();
		}
		bind(JPA.class).asEagerSingleton();
	}

	private Properties updatePersistenceProperties(Properties properties) {

		if (DBHelper.isDataSourceUsed()) {
			return properties;
		}

		final AppSettings settings = AppSettings.get();
		final Map<String, String> keys = new HashMap<>();
		final String unit = jpaUnit.replaceAll("(PU|Unit)$", "").replaceAll("^persistence$", "default");

		keys.put("db.%s.dialect", "hibernate.dialect");
		keys.put("db.%s.driver", "javax.persistence.jdbc.driver");
		keys.put("db.%s.ddl", "hibernate.hbm2ddl.auto");
		keys.put("db.%s.url", "javax.persistence.jdbc.url");
		keys.put("db.%s.user", "javax.persistence.jdbc.user");
		keys.put("db.%s.password", "javax.persistence.jdbc.password");

		for (String key : keys.keySet()) {
			String name = keys.get(key);
			String value = settings.get(String.format(key, unit));
			if (!StringUtils.isBlank(value)) {
				properties.put(name, value.trim());
			}
		}
		
		return properties;
	}
	
	private Properties updateCacheProperties(Properties properties) throws IOException {
		final Properties config = new Properties();
		config.load(ClassUtils.getResourceStream("ehcache-objects.properties"));

		for (Object key : config.keySet()) {
			String name = (String) key;
			String value = config.getProperty((String) name).trim();
			String prefix = "hibernate.ejb.classcache";
			if (!Character.isUpperCase(name.charAt(name.lastIndexOf(".") + 1))) {
				prefix = "hibernate.ejb.collectioncache";
			}
			properties.put(prefix + "." + name, value);
		}

		return properties;
	}

	public static class Initializer {

		@Inject
		Initializer(PersistService service) {
			log.info("Initialize JPA...");
			service.start();
		}
	}
}
