package com.axelor.web;

import java.io.InputStream;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.axelor.auth.db.User;
import com.fasterxml.jackson.databind.ObjectMapper;

@Singleton
public class AppSettings {

	private Properties properties;

	private static AppSettings INSTANCE;

	@Inject
	private AppSettings() {
			
		InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("application.properties");

		if (is == null) {
			throw new RuntimeException(
					"Unable to locate application configuration file.");
		}

		properties = new Properties();
		
		try {
			properties.load(is);
		} catch (Exception e) {
			throw new RuntimeException("Error reading application configuration.");
		}
	}

	public static AppSettings get() {
		if (INSTANCE == null)
			INSTANCE = new AppSettings();
		return INSTANCE;
	}

	public String get(String key) {
		return properties.getProperty(key);
	}

	public String get(String key, String defaultValue) {
		String value = properties.getProperty(key, defaultValue);
		if (value == null || "".equals(value.trim()))
			return defaultValue;
		return value;
	}
	
	public int getInt(String key, int defaultValue) {
		try {
			return Integer.parseInt(get(key).toString());
		} catch (Exception e){}
		return defaultValue;
	}
	
	public boolean getBoolean(String key, boolean defaultValue) {
		try {
			return Boolean.parseBoolean(get(key).toString());
		} catch (Exception e){}
		return defaultValue;
	}
	
	public void putAll(Properties properties) {
		this.properties.putAll(properties);
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	public String toJSON() {
		
		Properties settings = new Properties();
		Properties context = new Properties();
		
		Properties all = new Properties();
		
		try {
			Subject subject = SecurityUtils.getSubject();
			User user = User.all().filter("self.code = ?1", subject.getPrincipal()).fetchOne();
			context.put("__user__", user.getCode());
			settings.put("user.name", user.getName());
			settings.put("user.login", user.getCode());
		} catch (Exception e){
		}
		
		settings.putAll(properties);
		
		// remove server only properties
		settings.remove("temp.dir");
		
		all.put("appSettings", settings);
		all.put("appContext", context);
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(all);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}
}
