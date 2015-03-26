/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.joda.time.LocalDateTime;

import com.axelor.auth.db.AuditableModel;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;

@SuppressWarnings("serial")
public class AuditInterceptor extends EmptyInterceptor {

	private ThreadLocal<User> currentUser = new ThreadLocal<User>();

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

	@Override
	public boolean onFlushDirty(Object entity, Serializable id,
			Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
		if (!(entity instanceof AuditableModel)) {
			return false;
		}
		User user = this.getUser();
		for (int i = 0; i < propertyNames.length; i++) {
			if ("updatedOn".equals(propertyNames[i])) {
				currentState[i] = new LocalDateTime();
			}
			if ("updatedBy".equals(propertyNames[i]) && user != null) {
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
			if ("createdOn".equals(propertyNames[i])) {
				state[i] = new LocalDateTime();
			}
			if ("createdBy".equals(propertyNames[i]) && user != null) {
				state[i] = user;
			}
		}
		return true;
	}
}