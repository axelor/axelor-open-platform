package com.axelor.db;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.inject.AbstractModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.JpaPersistModule;

/**
 * A Guice module to configure JPA.
 * 
 * This module takes care of initialising JPA and registers an Hibernate custom
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
	
	private boolean isCacheEnabled() {
		try {
			InputStream res = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/persistence.xml");
			String text = CharStreams.toString(new InputStreamReader(res, Charsets.UTF_8));
			Pattern pat = Pattern.compile("<shared-cache-mode>\\s*(ENABLE_SELECTIVE|ALL)\\s*</shared-cache-mode>");
			Matcher mat = pat.matcher(text);
			return mat.find();
		} catch (Exception e) {}
		return false;
	}

	@Override
	protected void configure() {
		log.info("Initialize JPA...");
		Properties properties = new Properties();
		if (this.properties != null) {
			properties.putAll(this.properties);
		}
		if (this.autoscan) {
			properties.put("hibernate.ejb.resource_scanner", "com.axelor.db.JpaScanner");
		}
		
		properties.put("hibernate.connection.autocommit", "false");
		properties.put("hibernate.id.new_generator_mappings", "true");
		properties.put("hibernate.ejb.naming_strategy", "org.hibernate.cfg.ImprovedNamingStrategy");
		properties.put("hibernate.connection.charSet", "UTF-8");
		properties.put("hibernate.max_fetch_depth", "3");

		properties.put("jadira.usertype.autoRegisterUserTypes", "true");
		properties.put("jadira.usertype.databaseZone", "jvm");

		if (isCacheEnabled()) {
			properties.put("hibernate.cache.use_second_level_cache", "true");
			properties.put("hibernate.cache.use_query_cache", "true");
			properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
		}
		
		install(new JpaPersistModule(jpaUnit).properties(properties));
		if (this.autostart) {
			bind(Initializer.class).asEagerSingleton();
		}
		bind(JPA.class).asEagerSingleton();
	}

	public static class Initializer {

		@Inject
		Initializer(PersistService service) {
			service.start();
		}
	}
}
