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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.common.VersionUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.db.MetaFile;
import com.axelor.script.GroovyScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;

public class AppInfo {

	final AppSettings settings = AppSettings.get();

	public Map<String, Object> info() {

		final Map<String, Object> map = new HashMap<>();
		final User user = AuthUtils.getUser();

		if (user == null) {
			return map;
		}

		final Group group = user.getGroup();

		// if name field is overridden
		final Property nameField = Mapper.of(User.class).getNameField();
		final Object nameValue = nameField.get(user);

		map.put("user.id", user.getId());
		map.put("user.name", nameValue);
		map.put("user.login", user.getCode());
		map.put("user.nameField", nameField.getName());
		map.put("user.lang", user.getLanguage());
		map.put("user.action", user.getHomeAction());
		map.put("user.singleTab", user.getSingleTab());

		if (user.getImage() != null) {
			map.put("user.image", getLink(user, null));
		} else {
			map.put("user.image", "img/user.png");
		}

		if (group != null) {
			map.put("user.navigator", group.getNavigation());
			map.put("user.technical", group.getTechnicalStaff());
			map.put("user.group", group.getCode());
		}
		if (user.getHomeAction() == null && group != null) {
			map.put("user.action", group.getHomeAction());
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
		map.put("application.sdk", VersionUtils.getVersion().version);

		for (String key : settings.getProperties().stringPropertyNames()) {
			if (key.startsWith("view.")) {
				Object value = settings.get(key);
				if ("true".equals(value) || "false".equals(value)) {
					value = Boolean.parseBoolean(value.toString());
				}
				map.put(key, value);
			}
		}

		return map;
	}

	public String getLogo() throws JspException, IOException {
		final String logo = settings.get("application.logo", "img/axelor-logo.png");
		if (settings.get("context.appLogo") != null) {
			final ScriptBindings bindings = new ScriptBindings(new HashMap<String, Object>());
			final ScriptHelper helper = new GroovyScriptHelper(bindings);
			try {
				return getLink(helper.eval("__config__.appLogo"), logo);
			} catch (Exception e) {
			}
		}
		return logo;
	}

	private String getLink(Object value, String defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof String) {
			return (String) value;
		}
		if (value instanceof MetaFile) {
			return "ws/rest/" + MetaFile.class.getName() +"/" + ((MetaFile) value).getId() + "/content/download?v=" + ((MetaFile) value).getVersion();
		}
		if (value instanceof User) {
			return "ws/rest/" + User.class.getName() + "/" + ((User) value).getId() + "/image/download?image=true&v=" + ((User) value).getVersion();
		}
		return defaultValue;
	}
}
