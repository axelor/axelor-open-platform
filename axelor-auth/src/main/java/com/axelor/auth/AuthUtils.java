/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.auth;

import java.util.List;

import javax.persistence.FlushModeType;
import javax.persistence.TypedQuery;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.Subject;
import org.joda.time.LocalDate;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
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
		Subject subject = getSubject();
		if (subject == null || subject.getPrincipal() == null)
			return null;
		return getUser(subject.getPrincipal().toString());
	}

	public static User getUser(String code) {
		if (code == null) {
			return null;
		}
		return User.all().filter("self.code = ?", code)
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
