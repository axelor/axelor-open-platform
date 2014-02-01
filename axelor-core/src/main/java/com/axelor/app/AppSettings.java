/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.app;

import java.io.InputStream;
import java.util.Properties;

import com.axelor.common.ClassUtils;

public final class AppSettings {

	private Properties properties;

	private static AppSettings instance;

	private AppSettings() {

		final InputStream stream = ClassUtils.getResourceStream("application.properties");
		if (stream == null) {
			throw new RuntimeException("Unable to locate application configuration file.");
		}
		
		properties = new Properties();
		try {
			properties.load(stream);
		} catch (Exception e) {
			throw new RuntimeException("Error reading application configuration.");
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
