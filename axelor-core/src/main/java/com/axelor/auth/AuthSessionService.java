/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.auth.UserSession.Device;
import com.axelor.auth.db.User;
import com.axelor.inject.Beans;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;

/** Manages session attributes. */
public class AuthSessionService {
  public static final String LOGIN_DATE = "com.axelor.internal.loginDate";
  public static final String REMOTE_IP = "com.axelor.internal.remoteIp";
  public static final String USER_AGENT = "com.axelor.internal.userAgent";

  @Inject private UserAgentParser userAgentParser;

  public void updateLoginDate() {
    updateLoginDate(AuthUtils.getSubject().getSession(false));
  }

  public void updateLoginDate(Session session) {
    if (session != null) {
      session.setAttribute(LOGIN_DATE, LocalDateTime.now());
    }
  }

  @Nullable
  public LocalDateTime getLoginDate() {
    return getLoginDate(AuthUtils.getSubject().getSession(false));
  }

  @Nullable
  public LocalDateTime getLoginDate(Session session) {
    try {
      if (session != null) {
        return (LocalDateTime) session.getAttribute(LOGIN_DATE);
      }
    } catch (InvalidSessionException e) {
      // Fall through
    }

    return null;
  }

  public Collection<Session> getActiveSessions() {
    return Beans.get(SessionDAO.class).getActiveSessions();
  }

  /**
   * Returns all active sessions belonging to the given user.
   *
   * @param user the user whose sessions to retrieve
   * @return collection of active sessions for the user
   */
  public Collection<Session> getActiveSessions(User user) {
    return getActiveSessions().stream()
        .filter(
            session -> {
              Object principals =
                  session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
              if (principals instanceof PrincipalCollection principalCollection) {
                return user.getCode().equals(principalCollection.getPrimaryPrincipal().toString());
              }
              return false;
            })
        .collect(Collectors.toList());
  }

  /**
   * Revokes the session identified by the given session ID.
   *
   * @param sessionId the ID of the session to revoke
   */
  public void revokeSession(String sessionId) {
    revokeSession(
        new Subject.Builder(SecurityUtils.getSecurityManager())
            .sessionId(sessionId)
            .buildSubject());
  }

  /**
   * Revokes the given Shiro session.
   *
   * @param session the session to revoke
   */
  public void revokeSession(Session session) {
    revokeSession(session.getId().toString());
  }

  /**
   * Logs out the given subject, effectively revoking its session and firing logout events.
   *
   * @param subject the subject to log out
   */
  public void revokeSession(Subject subject) {
    subject.logout();
  }

  /**
   * Returns the list of active sessions for the given user, formatted for front-end consumption.
   *
   * <p>Each {@link UserSession} includes device info, timestamps as epoch milliseconds, and a flag
   * indicating whether it is the caller's current session.
   *
   * @param user the user whose sessions to retrieve
   * @return list of {@link UserSession} objects
   */
  public List<UserSession> getSessionsData(User user) {
    Subject subject = AuthUtils.getSubject();
    Session current = subject != null ? subject.getSession(false) : null;
    return getActiveSessions(user).stream()
        .map(session -> toUserSession(session, current))
        .toList();
  }

  private UserSession toUserSession(Session session, Session current) {
    String remoteIp = (String) session.getAttribute(REMOTE_IP);
    String rawUa = (String) session.getAttribute(USER_AGENT);

    UserAgentParser.UserAgentInfo ua = userAgentParser.parse(rawUa);
    Device device = ua != null ? Device.of(remoteIp, ua) : Device.ofIpOnly(remoteIp);

    LocalDateTime loginDate = (LocalDateTime) session.getAttribute(LOGIN_DATE);

    return new UserSession(
        session.getId().toString(),
        loginDate != null
            ? loginDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            : 0L,
        session.getLastAccessTime().toInstant().toEpochMilli(),
        current != null && session.getId().equals(current.getId()),
        device);
  }
}
