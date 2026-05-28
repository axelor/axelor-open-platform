/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.service;

import static org.apache.shiro.subject.support.DefaultSubjectContext.AUTHENTICATED_SESSION_KEY;
import static org.apache.shiro.subject.support.DefaultSubjectContext.PRINCIPALS_SESSION_KEY;

import com.axelor.auth.AuthSessionService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.inject.Beans;
import com.axelor.meta.theme.AvailableTheme;
import com.axelor.meta.theme.MetaThemeService;
import com.google.inject.servlet.RequestScoped;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.shiro.session.Session;

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
      final Collection<Session> sessions = Beans.get(AuthSessionService.class).getActiveSessions();
      final List<Map<String, Object>> users = new ArrayList<>();

      for (Session session : sessions) {
        try {
          if (session == null
              || session.getAttribute(PRINCIPALS_SESSION_KEY) == null
              || !Boolean.TRUE.equals(session.getAttribute(AUTHENTICATED_SESSION_KEY))) {
            continue;
          }
        } catch (IllegalStateException e) {
          // invalid session
          continue;
        }
        String login = session.getAttribute(PRINCIPALS_SESSION_KEY).toString();
        Map<String, Object> map = new HashMap<>();
        map.put("user", login);
        map.put("loginTime", session.getStartTimestamp());
        map.put("accessTime", session.getLastAccessTime());
        users.add(map);
      }

      info.put("osName", System.getProperty("os.name"));
      info.put("osArch", System.getProperty("os.arch"));
      info.put("osVersion", System.getProperty("os.version"));

      info.put("javaRuntime", System.getProperty("java.runtime.name"));
      info.put("javaVersion", System.getProperty("java.runtime.version"));

      info.put("memTotal", runtime.totalMemory());
      info.put("memMax", runtime.maxMemory());
      info.put("memFree", runtime.freeMemory());

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
