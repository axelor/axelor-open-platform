/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.web;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.meta.db.MetaUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Singleton
public class AppSettings {

	private Properties properties;
	private static Locale DEFAULT_LOCALE = new Locale("en");
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

	public String getPath(String key, String defaultValue) {
		String path = get(key, defaultValue);
		if (path == null) {
			return null;
		}
		return path.replace("{java.io.tmpdir}",
				System.getProperty("java.io.tmpdir")).replace("{user.home}",
				System.getProperty("user.home"));
	}

	public void putAll(Properties properties) {
		this.properties.putAll(properties);
	}

	public Properties getProperties() {
		return properties;
	}

	public String toJSON() {

		Map<String, Object> settings = Maps.newHashMap();

		try {
			User user = AuthUtils.getUser();
			Group group = user.getGroup();
			MetaUser prefs = MetaUser.findByUser(user);

			settings.put("user.name", user.getName());
			settings.put("user.login", user.getCode());

			if (group != null) {
				settings.put("user.navigator", group.getNavigation());
			}
			if (prefs != null) {
				settings.put("user.lang", prefs.getLanguage());
				settings.put("user.action", prefs.getAction().getName());
			}
		} catch (Exception e){
		}

		for(Object key : properties.keySet()) {
			settings.put((String) key, properties.get(key));
		}

		// remove server only properties
		settings.remove("temp.dir");

		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(settings);
		} catch (Exception e) {
		}
		return "{}";
	}

	private static String getUserLanguage() {

		final User user = AuthUtils.getUser();
		final TypedQuery<String> query = JPA.em().createQuery(
				"SELECT s.language FROM MetaUser s WHERE s.user.code = :code",
				String.class);

		if (user == null) {
			return null;
		}

		query.setMaxResults(1);
		query.setParameter("code", user.getCode());

		try {
			return query.getSingleResult();
		} catch (NoResultException e) {
		}

		return null;
	}

	/**
	 * Return the preferred language.
	 * Check if the locale JS file exist for the current language.
	 * By default, return the english file.
	 *
	 * @param request
	 * @param context
	 * @return language
	 */
	public static String getLocaleJS(HttpServletRequest request, ServletContext context){

		Locale locale = null;
		String language = getUserLanguage();

		if (!Strings.isNullOrEmpty(language)) {
			locale = toLocale(language);
		}
		if (locale == null) {
			locale = request.getLocale();
		}
		if (locale == null) {
			locale = toLocale(get().get("application.locale", DEFAULT_LOCALE.getLanguage()));
		}

		for(String lang : Lists.newArrayList(toLanguage(locale, false), toLanguage(locale, true))) {
			if (checkResources(context, "/js/i18n/" + lang + ".js")) {
				return lang;
			}
		}

		return DEFAULT_LOCALE.getLanguage();

	}

	/**
	 * Return the path of the JS file.
	 * If dev is specified in application.mode or if the minify JS file doesn't exist then return the unminify js file.
	 *
	 * @param context
	 * @return path of the JS file
	 */
	public static String getAppJS(ServletContext context) {
		String appJs = "js/application-all.min.js";

		if ("dev".equals(AppSettings.get().get("application.mode", "dev")) || checkResources(context, "/" + appJs) == false) {
			appJs = "js/application.js";
		}

		return appJs;
	}

	private static String toLanguage(Locale locale, boolean minimize) {
		StringBuilder format = new StringBuilder(locale.getLanguage().toLowerCase());
		if(!minimize && !Strings.isNullOrEmpty(locale.getCountry()))
			format.append("_").append(locale.getCountry().toUpperCase());
		return format.toString();
	}

	private static Locale toLocale(String language) {
	    String parts[] = language.split("_", -1);
	    if (parts.length == 1) return new Locale(parts[0].toLowerCase());
	    return new Locale(parts[0].toLowerCase(), parts[1].toUpperCase());
	}

	private static boolean checkResources(ServletContext context, String resourcesPath) {
		try{
			URL path = context.getResource(resourcesPath);
			return path == null ? false : true;
		} catch(MalformedURLException e){
			return false;
		}
	}
}
