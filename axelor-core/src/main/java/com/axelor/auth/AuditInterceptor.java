/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.PersistenceException;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

import com.axelor.auth.db.AuditableModel;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.db.EntityHelper;
import com.axelor.db.JPA;
import com.axelor.db.JpaSequence;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.db.MetaSequence;

@SuppressWarnings("serial")
public class AuditInterceptor extends EmptyInterceptor {

	private final ThreadLocal<User> currentUser = new ThreadLocal<User>();
	private final ThreadLocal<AuditTracker> tracker = new ThreadLocal<>();

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
		tracker.set(new AuditTracker());
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		tracker.remove();
		currentUser.remove();
	}

	@Override
	public void beforeTransactionCompletion(Transaction tx) {
		tracker.get().onComplete(tx, getUser());
	}

	private User getUser() {
		User user = currentUser.get();
		if (user == null) {
			user = AuditableRunner.batchUser.get();
		}
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
			if (entity instanceof User && ADMIN_USER.equals(prevValue) && !ADMIN_USER.equals(newValue)) {
				return false;
			}
			if (entity instanceof Group && ADMIN_GROUP.equals(prevValue) && !ADMIN_GROUP.equals(newValue)) {
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

	private boolean updateSequence(Object entity, String[] names, Object[] state) {
		if ((entity instanceof MetaSequence) || !(entity instanceof Model)) {
			return false;
		}

		final Mapper mapper = Mapper.of(EntityHelper.getEntityClass(entity));
		boolean updated = false;

		for (int i = 0; i < names.length; i++) {
			if (state[i] != null) {
				continue;
			}
			final Property property = mapper.getProperty(names[i]);
			if (property != null && property.isSequence()) {
				state[i] = JpaSequence.nextValue(property.getSequenceName());
				updated = true;
			}
		}

		return updated;
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {

		if (!(entity instanceof AuditableModel)) {
			return false;
		}

		final User user = this.getUser();
		for (int i = 0; i < propertyNames.length; i++) {
			if (!canUpdate(entity, propertyNames[i], previousState[i], currentState[i])) {
				throw new PersistenceException(String.format("You can't update: %s#%s, values (%s=%s)",
						entity.getClass().getName(), id, propertyNames[i], currentState[i]));
			}
			if (UPDATED_ON.equals(propertyNames[i])) {
				currentState[i] = LocalDateTime.now();
			}
			if (UPDATED_BY.equals(propertyNames[i]) && user != null) {
				currentState[i] = user;
			}
		}

		// change tracking
		if (tracker.get() != null) {
			tracker.get().track((AuditableModel) entity, propertyNames, currentState, previousState);
		}

		return true;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {

		boolean changed = updateSequence(entity, propertyNames, state);
		if (!(entity instanceof AuditableModel)) {
			return changed;
		}

		final User user = this.getUser();
		for (int i = 0; i < propertyNames.length; i++) {
			if (state[i] != null) {
				continue;
			}
			if (CREATED_ON.equals(propertyNames[i])) {
				state[i] = LocalDateTime.now();
			}
			if (CREATED_BY.equals(propertyNames[i]) && user != null) {
				state[i] = user;
			}
		}

		// change tracking
		if (tracker.get() != null) {
			tracker.get().track((AuditableModel) entity, propertyNames, state, null);
		}

		return true;
	}

	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (!canDelete(entity)) {
			throw new PersistenceException(String.format("You can't delete: %s#%s", entity.getClass().getName(), id));
		}
	}
}
