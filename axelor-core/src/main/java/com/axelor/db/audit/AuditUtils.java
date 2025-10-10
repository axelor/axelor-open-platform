/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import java.util.Optional;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SessionImplementor;

final class AuditUtils {

  static User findUser(SessionImplementor session, String code) {
    return session
        .createQuery("SELECT self FROM User self WHERE self.code = :code", User.class)
        .setParameter("code", code)
        .setHibernateFlushMode(FlushMode.MANUAL)
        .setCacheMode(CacheMode.NORMAL)
        .uniqueResult();
  }

  static User currentUser(SessionImplementor session) {
    User user = AuthUtils.getCurrentUser();
    if (user == null) {
      String code =
          Optional.ofNullable(AuthUtils.getSubject())
              .map(x -> x.getPrincipal())
              .map(x -> x.toString())
              .orElse(null);
      user = findUser(session, code);
    }

    if (user == null) return null;
    if (session.contains(user)) {
      return user;
    }

    return findUser(session, user.getCode());
  }
}
