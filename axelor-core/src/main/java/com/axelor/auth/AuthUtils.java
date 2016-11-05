/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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
package com.axelor.auth;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.FlushModeType;
import javax.persistence.TypedQuery;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.subject.Subject;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.QueryBinder;

public class AuthUtils {

	public static Subject getSubject() {
		try {
			return SecurityUtils.getSubject();
		} catch (UnavailableSecurityManagerException e){
		}
		return null;
	}

	public static User getUser() {
		try {
			return getUser(getSubject().getPrincipal().toString());
		} catch (NullPointerException | InvalidSessionException e) {
		}
		return null;
	}

	public static User getUser(String code) {
		if (code == null) {
			return null;
		}
		return JpaRepository.of(User.class).all()
				.filter("self.code = ?", code)
				.cacheable().autoFlush(false).fetchOne();
	}
	
	public static boolean isActive(final User user) {
		if (user.getArchived() == Boolean.TRUE ||
			user.getBlocked() == Boolean.TRUE) {
			return false;
		}
		
		final LocalDate from = user.getActivateOn();
		final LocalDate till = user.getExpiresOn();
		final LocalDate now = LocalDate.now();
		
		if ((from != null && from.isAfter(now)) ||
			(till != null && till.isBefore(now))) {
			return false;
		}
		
		return true;
	}

	public static boolean isAdmin(final User user) {
		return "admin".equals(user.getCode())
				|| (user.getGroup() != null && "admins".equals(user.getGroup().getCode()));
	}

	public static boolean isTechnicalStaff(final User user) {
		return user.getGroup() != null && user.getGroup().getTechnicalStaff() == Boolean.TRUE;
	}

	private static final String QS_HAS_ROLE = "SELECT self.id FROM Role self WHERE "
			+ "(self.name = :name) AND "
			+ "("
			+ "  (self.id IN (SELECT r.id FROM User u LEFT JOIN u.roles AS r WHERE u.code = :user)) OR "
			+ "  (self.id IN (SELECT r.id FROM User u LEFT JOIN u.group AS g LEFT JOIN g.roles AS r WHERE u.code = :user))"
			+ ")";
	
	public static boolean hasRole(final User user, final String role) {
		final TypedQuery<Long> query = JPA.em().createQuery(QS_HAS_ROLE, Long.class);
		query.setParameter("name", role);
		query.setParameter("user", user.getCode());
		query.setMaxResults(1);
		
		QueryBinder.of(query).opts(true, FlushModeType.COMMIT);
		
		final List<Long> ids = query.getResultList();
		return ids != null && ids.size() == 1;
	}
}
