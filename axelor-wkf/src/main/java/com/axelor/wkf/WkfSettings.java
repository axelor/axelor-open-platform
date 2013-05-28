package com.axelor.wkf;

import java.io.InputStream;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WkfSettings {

	private Properties properties;

	private static WkfSettings INSTANCE;

	@Inject
	private WkfSettings() {
			
		InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("wkf.properties");

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

	public static WkfSettings get() {
		if (INSTANCE == null)
			INSTANCE = new WkfSettings();
		return INSTANCE;
	}

	public String get(String key) {
		return properties.getProperty(key);
	}

	public String get(String key, String defaultValue) {
		String value = properties.getProperty(key, defaultValue);
		if (value == null || "".equals(value.trim()))
			return defaultValue;
		return value.trim();
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
}
