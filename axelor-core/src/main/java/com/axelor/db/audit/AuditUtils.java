/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import java.util.Optional;
import org.apache.shiro.subject.Subject;
import org.hibernate.engine.spi.SessionImplementor;

final class AuditUtils {

  static final String UPDATED_BY = "updatedBy";
  static final String UPDATED_ON = "updatedOn";
  static final String CREATED_BY = "createdBy";
  static final String CREATED_ON = "createdOn";

  static User findUser(SessionImplementor session, Long id) {
    if (id == null) {
      return null;
    }
    return session.find(User.class, id);
  }

  static User currentUser(SessionImplementor session) {
    User user = AuthUtils.getCurrentUser();
    if (user == null) {
      Long userId =
          Optional.ofNullable(AuthUtils.getSubject())
              .map(Subject::getPrincipals)
              .map(x -> x.oneByType(Long.class))
              .orElse(null);
      user = findUser(session, userId);
    }

    if (user == null) return null;
    if (session.contains(user)) {
      return user;
    }

    return findUser(session, user.getId());
  }
}
