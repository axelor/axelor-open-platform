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
package com.axelor.db.audit;

import com.axelor.auth.AuditableRunner;
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
    User user = AuditableRunner.batchUser();
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
