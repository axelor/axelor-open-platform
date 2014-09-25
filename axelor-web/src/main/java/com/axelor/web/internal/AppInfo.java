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
package com.axelor.web.internal;

import java.net.MalformedURLException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.common.VersionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public final class AppInfo {

	private static final String APPLICATION_JS = "js/application.js";
	private static final String APPLICATION_JS_MIN = "js/application.min.js";
	
	private static final String APPLICATION_CSS = "css/application.css";
	private static final String APPLICATION_CSS_MIN = "css/application.min.css";

	private static final String APPLICATION_LANG_JS = "js/i18n/%s.js";
	
	private static final Locale DEFAULT_LOCALE = new Locale("en");
	
	private static final Pattern MOBILE_PATTERN = Pattern.compile("(Mobile|Android|iPhone|iPad|iPod|BlackBerry|Windows Phone)", Pattern.CASE_INSENSITIVE);
	private static final Pattern WEBKIT_PATTERN = Pattern.compile("WebKit", Pattern.CASE_INSENSITIVE);

	private static final AppSettings settings = AppSettings.get();

	public static String asJson() {

		final Map<String, Object> map = Maps.newHashMap();
		try {
			User user = AuthUtils.getUser();
			Group group = user.getGroup();

			map.put("user.name", user.getName());
			map.put("user.login", user.getCode());

			if (group != null) {
				map.put("user.navigator", group.getNavigation());
				map.put("user.technical", group.getTechnicalStaff());
				map.put("user.group", group.getCode());
			}
			map.put("user.lang", user.getLanguage());
			map.put("user.action", user.getHomeAction());
		} catch (Exception e){
		}

		map.put("application.name", settings.get("application.name"));
		map.put("application.description", settings.get("application.description"));
		map.put("application.version", settings.get("application.version"));
		map.put("application.author", settings.get("application.author"));
		map.put("application.copyright", settings.get("application.copyright"));
		map.put("application.home", settings.get("application.home"));
		map.put("application.help", settings.get("application.help"));
		map.put("application.mode", settings.get("application.mode"));

		map.put("file.upload.size", settings.get("file.upload.size"));

		try {
			map.put("application.sdk", VersionUtils.getVersion().version);
			map.put("application.version", VersionUtils.getVersion(settings.get("application.name")).version);
		} catch (Exception e) {
		}

		for (String key : settings.getProperties().stringPropertyNames()) {
			if (key.startsWith("view.")) {
				Object value = settings.get(key);
				if ("true".equals(value) || "false".equals(value)) {
					value = Boolean.parseBoolean(value.toString());
				}
				map.put(key, value);
			}
		}

		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(map);
		} catch (Exception e) {
		}
		return "{}";
	}

	private static String getUserLanguage() {
		final User user = AuthUtils.getUser();
		if (user == null) {
			return null;
		}
		return user.getLanguage();
	}

	public static String getLangJS(HttpServletRequest request, ServletContext context){

		Locale locale = null;
		String language = getUserLanguage();
		
		if (!StringUtils.isBlank(language)) {
			locale = toLocale(language);
		}
		if (locale == null) {
			locale = request.getLocale();
		}
		if (locale == null) {
			locale = toLocale(settings.get("application.locale", DEFAULT_LOCALE.getLanguage()));
		}

		for(String lang : Lists.newArrayList(toLanguage(locale, false), toLanguage(locale, true))) {
			if (checkResources(context, "/js/i18n/" + lang + ".js")) {
				language = lang;
				break;
			}
		}
		
		if (language == null) {
			language = DEFAULT_LOCALE.getLanguage();
		}
		
		return String.format(APPLICATION_LANG_JS, language);
	}

	public static boolean isMobile(HttpServletRequest request) {
		String agent = request.getHeader("user-agent");
		if (agent == null) {
			return false;
		}
		return MOBILE_PATTERN.matcher(agent).find();
	}

	public static boolean isWebKit(HttpServletRequest request) {
		String agent = request.getHeader("user-agent");
		if (agent == null) {
			return false;
		}
		return WEBKIT_PATTERN.matcher(agent).find();
	}

	public static String getAppJS(ServletContext context) {
		if (settings.isProduction() && checkResources(context, "/" + APPLICATION_JS_MIN)) {
			return APPLICATION_JS_MIN;
		}
		return APPLICATION_JS;
	}
	
	public static String getAppCSS(ServletContext context) {
		if (settings.isProduction() && checkResources(context, APPLICATION_CSS_MIN)) {
			return APPLICATION_CSS_MIN;
		}
		return APPLICATION_CSS;
	}

	private static String toLanguage(Locale locale, boolean minimize) {
		final String lang = locale.getLanguage().toLowerCase();
		if (minimize || StringUtils.isBlank(locale.getCountry())) {
			return lang;
		}
		return lang + "_" + locale.getCountry().toUpperCase();
	}

	private static Locale toLocale(String language) {
	    final String parts[] = language.split("_", -1);
	    if (parts.length == 1) {
	    	return new Locale(parts[0].toLowerCase());
	    }
	    return new Locale(parts[0].toLowerCase(), parts[1].toUpperCase());
	}

	private static boolean checkResources(ServletContext context, String resourcesPath) {
		try {
			return context.getResource(resourcesPath) != null;
		} catch (MalformedURLException e) {
			return false;
		}
	}
}
