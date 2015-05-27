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
package com.axelor.web.internal;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.common.VersionUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

public final class AppInfo {

	private static final String APPLICATION_JS = "js/application.js";
	private static final String APPLICATION_JS_MIN = "js/application.min.js";
	
	private static final String APPLICATION_CSS = "css/application.css";
	private static final String APPLICATION_CSS_MIN = "css/application.min.css";

	private static final Pattern MOBILE_PATTERN = Pattern.compile("(Mobile|Android|iPhone|iPad|iPod|BlackBerry|Windows Phone)", Pattern.CASE_INSENSITIVE);
	private static final Pattern WEBKIT_PATTERN = Pattern.compile("WebKit", Pattern.CASE_INSENSITIVE);

	private static final AppSettings settings = AppSettings.get();

	public static String asJson() {

		final Map<String, Object> map = Maps.newHashMap();
		try {
			User user = AuthUtils.getUser();
			Group group = user.getGroup();

			// if name field is overridden
			Property nameField = Mapper.of(User.class).getNameField();
			Object nameValue = nameField.get(user);

			map.put("user.id", user.getId());
			map.put("user.name", nameValue);
			map.put("user.login", user.getCode());
			map.put("user.nameField", nameField.getName());

			if (user.getImage() != null) {
				map.put("user.image", "ws/rest/" + User.class.getName() + "/" + user.getId() + "/image/download?image=true&v=" + user.getVersion());
			} else {
				map.put("user.image", "img/user.png");
			}

			if (group != null) {
				map.put("user.navigator", group.getNavigation());
				map.put("user.technical", group.getTechnicalStaff());
				map.put("user.group", group.getCode());
			}
			map.put("user.lang", user.getLanguage());
			map.put("user.action", user.getHomeAction());

			if (user.getHomeAction() == null) {
				map.put("user.action", group.getHomeAction());
			}
		} catch (Exception e){
		}

		map.put("application.name", settings.get("application.name"));
		map.put("application.description", settings.get("application.description"));
		map.put("application.version", settings.get("application.version"));
		map.put("application.author", settings.get("application.author"));
		map.put("application.copyright", settings.get("application.copyright"));
		map.put("application.home", settings.get("application.home"));
		map.put("application.help", settings.get("application.help"));
		map.put("application.mode", settings.get("application.mode", "dev"));

		map.put("file.upload.size", settings.get("file.upload.size", "5"));

		try {
			map.put("application.sdk", VersionUtils.getVersion().version);
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

	private static boolean checkResources(ServletContext context, String resourcesPath) {
		try {
			return context.getResource(resourcesPath) != null;
		} catch (MalformedURLException e) {
			return false;
		}
	}
}
