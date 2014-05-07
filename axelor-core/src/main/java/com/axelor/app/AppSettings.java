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
package com.axelor.app;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import com.axelor.common.ClassUtils;
import com.axelor.common.StringUtils;

public final class AppSettings {

	private static final String DEFAULT_CONFIG_LOCATION = "application.properties";
	private static final String CUSTOM_CONFIG_LOCATION = "axelor.config";

	private Properties properties;

	private static AppSettings instance;

	private AppSettings() {
		String config = System.getProperty(CUSTOM_CONFIG_LOCATION);
		InputStream stream = null;
		try {
			if (StringUtils.isBlank(config)) {
				stream = ClassUtils.getResourceStream(config = DEFAULT_CONFIG_LOCATION);
			} else {
				stream = new FileInputStream(config);
			}
			try {
				properties = new Properties();
				properties.load(stream);
			} finally {
				stream.close();
			}
		} catch (Exception e) {
			throw new RuntimeException("Unable to load application settings: " + config);
		}
	}

	public static AppSettings get() {
		if (instance == null) {
			instance = new AppSettings();
		}
		return instance;
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

	public String getPath(String key, String defaultValue) {
		String path = get(key, defaultValue);
		if (path == null) {
			return null;
		}
		return path.replace("{java.io.tmpdir}",
				System.getProperty("java.io.tmpdir")).replace("{user.home}",
				System.getProperty("user.home"));
	}

	public String getBaseURL() {
		String appUrl = get("application.baseUrl");
		String reqUrl = get("application.request.baseUrl", appUrl);
		return reqUrl;
	}

	public boolean isProduction() {
		return !"dev".equals(get("application.mode", "dev"));
	}

	public void putAll(Properties properties) {
		this.properties.putAll(properties);
	}

	public Properties getProperties() {
		return properties;
	}
}
