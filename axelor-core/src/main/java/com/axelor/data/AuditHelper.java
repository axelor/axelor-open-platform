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
package com.axelor.data;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import com.axelor.auth.db.AuditableModel;
import com.axelor.auth.db.User;
import com.axelor.db.mapper.types.JavaTimeAdapter;

public class AuditHelper {

	private static final String CREATED_ON = "createdOn";
	private static final String CREATED_BY = "createdBy";
	private static final String UPDATED_ON = "updatedOn";
	private static final String UPDATED_BY = "updatedBy";

	private static final String SET_CREATED_ON = "setCreatedOn";
	private static final String SET_CREATED_BY = "setCreatedBy";
	private static final String SET_UPDATED_ON = "setUpdatedOn";
	private static final String SET_UPDATED_BY = "setUpdatedBy";

	private static Method createdOn;
	private static Method createdBy;
	private static Method updatedOn;
	private static Method updatedBy;

	private static JavaTimeAdapter adapter;

	private AuditHelper() {
	}

	private static void init() {
		adapter = new JavaTimeAdapter();
		try {
			createdOn = AuditableModel.class.getDeclaredMethod(SET_CREATED_ON, LocalDateTime.class);
			createdBy = AuditableModel.class.getDeclaredMethod(SET_CREATED_BY, User.class);
			updatedOn = AuditableModel.class.getDeclaredMethod(SET_UPDATED_ON, LocalDateTime.class);
			updatedBy = AuditableModel.class.getDeclaredMethod(SET_UPDATED_BY, User.class);

			createdOn.setAccessible(true);
			createdBy.setAccessible(true);
			updatedOn.setAccessible(true);
			updatedBy.setAccessible(true);
		} catch (Exception e) {
		}
	}

	private static Object adapt(Object value) {
		return adapter.adapt(value, LocalDateTime.class, LocalDateTime.class, null);
	}

	private static Object invoke(Object bean, Method method, Object value) {
		try {
			return method.invoke(bean, value);
		} catch (Exception e) {
		}
		return value;
	}

	public static boolean update(Object bean, String field, Object value) {

		if (bean == null || !AuditableModel.class.isInstance(bean)) {
			return false;
		}

		if (adapter == null) {
			init();
		}

		switch (field) {
		case CREATED_BY:
			invoke(bean, createdBy, value);
			break;
		case UPDATED_BY:
			invoke(bean, updatedBy, value);
			break;
		case CREATED_ON:
			invoke(bean, createdOn, adapt(value));
			break;
		case UPDATED_ON:
			invoke(bean, updatedOn, adapt(value));
			break;
		default:
			return false;
		}
		return true;
	}
}
