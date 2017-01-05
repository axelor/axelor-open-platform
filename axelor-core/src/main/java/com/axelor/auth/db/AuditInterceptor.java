/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.auth.db;

import java.io.Serializable;

import javax.persistence.PersistenceException;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.joda.time.LocalDateTime;

import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.i18n.I18n;

@SuppressWarnings("serial")
public class AuditInterceptor extends EmptyInterceptor {

	private ThreadLocal<User> currentUser = new ThreadLocal<User>();

	private static final String UPDATED_BY = "updatedBy";
	private static final String UPDATED_ON = "updatedOn";
	private static final String CREATED_BY = "createdBy";
	private static final String CREATED_ON = "createdOn";

	private static final String ADMIN_USER = "admin";
	private static final String ADMIN_GROUP = "admins";
	private static final String ADMIN_CHECK_FIELD = "code";

	@Override
	public void afterTransactionBegin(Transaction tx) {
		currentUser.set(AuthUtils.getUser());
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		currentUser.remove();
	}

	private User getUser() {
		User user = currentUser.get();
		if (user == null || JPA.em().contains(user)) {
			return user;
		}

		user = AuthUtils.getUser(user.getCode());
		if (user == null) {
			return null;
		}

		currentUser.remove();
		currentUser.set(user);

		return user;
	}

	private boolean canUpdate(Object entity, String field, Object prevValue, Object newValue) {
		if (!(entity instanceof Model) || ((Model) entity).getId() == null) {
			return true;
		}
		if (entity instanceof User || entity instanceof Group) {
			if (!ADMIN_CHECK_FIELD.equals(field)) {
				return true;
			}
			if (entity instanceof User &&
					ADMIN_USER.equals(prevValue) &&
					!ADMIN_USER.equals(newValue)) {
				return false;
			}
			if (entity instanceof Group &&
					ADMIN_GROUP.equals(prevValue) &&
					!ADMIN_GROUP.equals(newValue)) {
				return false;
			}
		}
		return true;
	}

	private boolean canDelete(Object entity) {
		if (entity instanceof User && ADMIN_USER.equals(((User) entity).getCode())) {
			return false;
		}
		if (entity instanceof Group && ADMIN_GROUP.equals(((Group) entity).getCode())) {
			return false;
		}
		return true;
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id,
			Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
		if (!(entity instanceof AuditableModel)) {
			return false;
		}
		User user = this.getUser();
		for (int i = 0; i < propertyNames.length; i++) {
			if (!canUpdate(entity, propertyNames[i], previousState[i], currentState[i])) {
				throw new PersistenceException(
						String.format(I18n.get("You can't update: %s#%s, values (%s=%s)"),
								entity.getClass().getName(), id,
								propertyNames[i], currentState[i]));
			}
			if (UPDATED_ON.equals(propertyNames[i])) {
				currentState[i] = new LocalDateTime();
			}
			if (UPDATED_BY.equals(propertyNames[i]) && user != null) {
				currentState[i] = user;
			}
		}
		return true;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types) {
		if (!(entity instanceof AuditableModel)) {
			return false;
		}
		User user = this.getUser();
		for (int i = 0; i < propertyNames.length; i++) {
			if (state[i] != null) {
				continue;
			}
			if (CREATED_ON.equals(propertyNames[i])) {
				state[i] = new LocalDateTime();
			}
			if (CREATED_BY.equals(propertyNames[i]) && user != null) {
				state[i] = user;
			}
		}
		return true;
	}
	
	@Override
	public void onDelete(Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types) {
		if (!canDelete(entity)) {
			throw new PersistenceException(
				String.format(I18n.get("You can't delete: %s#%s"),
					entity.getClass().getName(), id));
		}
		super.onDelete(entity, id, state, propertyNames, types);
	}
}