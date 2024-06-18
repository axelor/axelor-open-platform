/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.web.service;

import static java.lang.Boolean.TRUE;
import static org.apache.shiro.subject.support.DefaultSubjectContext.AUTHENTICATED_SESSION_KEY;
import static org.apache.shiro.subject.support.DefaultSubjectContext.PRINCIPALS_SESSION_KEY;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.inject.Beans;
import com.axelor.meta.theme.AvailableTheme;
import com.axelor.meta.theme.MetaThemeService;
import com.axelor.web.AppSessionListener;
import com.google.inject.servlet.RequestScoped;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/app")
@Hidden
public class AboutService extends AbstractService {

  @GET
  @Path("sysinfo")
  public Map<String, Object> getSystemInfo() {
    final Map<String, Object> info = new HashMap<>();
    final User user = AuthUtils.getUser();

    if (user != null && AuthUtils.isTechnicalStaff(user)) {
      final Runtime runtime = Runtime.getRuntime();
      final Set<HttpSession> sessions = AppSessionListener.getSessions();
      final List<Map<String, Object>> users = new ArrayList<>();

      int mb = 1024;

      for (HttpSession session : sessions) {
        try {
          if (session == null
              || session.getAttribute(PRINCIPALS_SESSION_KEY) == null
              || session.getAttribute(AUTHENTICATED_SESSION_KEY) != TRUE) {
            continue;
          }
        } catch (IllegalStateException e) {
          // invalid session
          continue;
        }
        String login = session.getAttribute(PRINCIPALS_SESSION_KEY).toString();
        Map<String, Object> map = new HashMap<>();
        map.put("user", login);
        map.put("loginTime", session.getCreationTime());
        map.put("accessTime", session.getLastAccessedTime());
        users.add(map);
      }

      info.put("osName", System.getProperty("os.name"));
      info.put("osArch", System.getProperty("os.arch"));
      info.put("osVersion", System.getProperty("os.version"));

      info.put("javaRuntime", System.getProperty("java.runtime.name"));
      info.put("javaVersion", System.getProperty("java.runtime.version"));

      info.put("memTotal", (runtime.totalMemory() / mb) + " Kb");
      info.put("memMax", (runtime.maxMemory() / mb) + " Kb");
      info.put("memUsed", ((runtime.totalMemory() - runtime.freeMemory()) / mb) + " Kb");
      info.put("memFree", (runtime.freeMemory() / mb) + " Kb");

      info.put("users", users);
    }

    return info;
  }

  /**
   * Retrieve themes available for users
   *
   * @return list of {@link AvailableTheme}
   */
  @GET
  @Path("themes")
  public List<AvailableTheme> getThemes() {
    List<AvailableTheme> availableThemes = new ArrayList<>();
    try {
      availableThemes = Beans.get(MetaThemeService.class).getAvailableThemes(AuthUtils.getUser());
    } catch (Exception e) {
      // ignore
    }
    return availableThemes;
  }
}
