/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
import com.axelor.db.tenants.TenantResolver;
import com.axelor.web.AppSessionListener;
import com.axelor.web.internal.AppInfo;
import com.google.inject.servlet.RequestScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/app")
public class AboutService extends AbstractService {

  @Context private HttpServletRequest request;

  @Context private HttpServletResponse response;

  @Inject private AppInfo info;

  /**
   * Retrieves application information.
   *
   * @deprecated Use /public/app/info instead.
   */
  @GET
  @Path("info")
  @Deprecated(since = "7.0.0", forRemoval = true)
  public Map<String, Object> info() {
    return info.info(request.getServletContext());
  }

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
              || session.getAttribute(AUTHENTICATED_SESSION_KEY) != TRUE
              || !TenantResolver.isCurrentTenantSession(session)) {
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
}
