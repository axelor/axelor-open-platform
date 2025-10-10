/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.QueryBinder;
import com.google.common.base.Preconditions;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.subject.Subject;

public class AuthUtils {

  private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

  /** For internal use only. */
  public static void setCurrentUser(User user) {
    CURRENT_USER.set(user);
  }

  /** For internal use only. */
  public static User getCurrentUser() {
    return CURRENT_USER.get();
  }

  /** For internal use only. */
  public static void removeCurrentUser() {
    CURRENT_USER.remove();
  }

  public static Subject getSubject() {
    try {
      return SecurityUtils.getSubject();
    } catch (UnavailableSecurityManagerException e) {
      // ignore
    }
    return null;
  }

  /**
   * Retrieves the current user associated with the thread or session.
   *
   * <p>It first uses the current user stored in the thread-local (set for batch / audit purpose) if
   * provided. Else it attempts to retrieve the user based on the principal associated with the
   * current subject
   *
   * @return the current user if available; otherwise null
   */
  public static User getUser() {
    final User currentUser = CURRENT_USER.get();
    if (currentUser != null) {
      return getUser(currentUser.getCode());
    }
    try {
      return getUser(getSubject().getPrincipal().toString());
    } catch (NullPointerException | InvalidSessionException e) {
      // ignore
    }
    return null;
  }

  public static User getUser(String code) {
    if (code == null) {
      return null;
    }
    return JpaRepository.of(User.class)
        .all()
        .filter("self.code = ?", code)
        .cacheable()
        .autoFlush(false)
        .fetchOne();
  }

  public static boolean isActive(final User user) {
    if (Boolean.TRUE.equals(user.getArchived()) || Boolean.TRUE.equals(user.getBlocked())) {
      return false;
    }

    final LocalDateTime from = user.getActivateOn();
    final LocalDateTime till = user.getExpiresOn();
    final LocalDateTime now = LocalDateTime.now();

    if ((from != null && from.isAfter(now)) || (till != null && till.isBefore(now))) {
      return false;
    }

    return true;
  }

  public static boolean isAdmin(final User user) {
    return "admin".equals(user.getCode())
        || (user.getGroup() != null && "admins".equals(user.getGroup().getCode()));
  }

  public static boolean isTechnicalStaff(final User user) {
    return user.getGroup() != null && Boolean.TRUE.equals(user.getGroup().getTechnicalStaff());
  }

  private static final String QS_HAS_ROLE =
      """
      SELECT self.id FROM Role self WHERE \
      (self.name IN (:roles)) AND \
      (\
        (self.id IN (SELECT r.id FROM User u LEFT JOIN u.roles AS r WHERE u.code = :user)) OR \
        (self.id IN (SELECT r.id FROM User u LEFT JOIN u.group AS g LEFT JOIN g.roles AS r WHERE u.code = :user))\
      )""";

  public static boolean hasRole(final User user, final String... roles) {
    Preconditions.checkArgument(user != null, "user not provided.");
    Preconditions.checkArgument(roles != null, "roles not provided.");
    Preconditions.checkArgument(roles.length > 0, "roles not provided.");
    final TypedQuery<Long> query = JPA.em().createQuery(QS_HAS_ROLE, Long.class);
    query.setParameter("roles", Arrays.asList(roles));
    query.setParameter("user", user.getCode());
    query.setMaxResults(1);

    QueryBinder.of(query).opts(true, FlushModeType.COMMIT);

    final List<Long> ids = query.getResultList();
    return ids != null && ids.size() == 1;
  }
}
