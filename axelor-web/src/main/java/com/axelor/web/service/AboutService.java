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
package com.axelor.web.service;

import static java.lang.Boolean.TRUE;
import static org.apache.shiro.subject.support.DefaultSubjectContext.AUTHENTICATED_SESSION_KEY;
import static org.apache.shiro.subject.support.DefaultSubjectContext.PRINCIPALS_SESSION_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.common.VersionUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.web.AppSessionListener;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/app")
public class AboutService extends AbstractService {

	@Context
	private HttpServletRequest  request;

	@Context
	private HttpServletResponse response;

	@GET
	@Path("info")
	public Map<String, Object> info() {

		final AppSettings settings = AppSettings.get();
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
			map.put("user.image", "ws/rest/" + User.class.getName() + "/" + user.getId() + "/image/download?image=true&v=" + user.getVersion());
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

	@GET
	@Path("sysinfo")
	public Properties getSystemInfo() {
		final Properties info = new Properties();
		final Runtime runtime = Runtime.getRuntime();
		final Set<String> sessions = AppSessionListener.getActiveSessions();
		final User user = AuthUtils.getUser();
		final List<Map<String,Object>> users = new ArrayList<>();

		int mb = 1024;

		final boolean isTechnicalStaff = user.getGroup() != null
				&& user.getGroup().getTechnicalStaff() == TRUE;

		for (String id : sessions) {
			HttpSession session = AppSessionListener.getSession(id);
			if (session == null ||
				session.getAttribute(PRINCIPALS_SESSION_KEY) == null ||
				session.getAttribute(AUTHENTICATED_SESSION_KEY) != TRUE) {
				continue;
			}
			String login = session.getAttribute(PRINCIPALS_SESSION_KEY).toString();
			Map<String, Object> map = new HashMap<>();
			map.put("user", login);
			map.put("loginTime", session.getCreationTime());
			map.put("accessTime", session.getLastAccessedTime());
			users.add(map);
		}

		info.setProperty("osName", System.getProperty("os.name"));
		info.setProperty("osArch", System.getProperty("os.arch"));
		info.setProperty("osVersion", System.getProperty("os.version"));

		info.setProperty("javaRuntime", System.getProperty("java.runtime.name"));
		info.setProperty("javaVersion", System.getProperty("java.runtime.version"));

		info.setProperty("memTotal", (runtime.totalMemory() / mb) + " Kb");
		info.setProperty("memMax", (runtime.maxMemory() / mb) + " Kb");
		info.setProperty("memUsed", ((runtime.totalMemory() - runtime.freeMemory()) / mb) + " Kb");
		info.setProperty("memFree", (runtime.freeMemory() / mb) + " Kb");

		if (isTechnicalStaff) {
			info.put("users", users);
		}

		return info;
	}
}
