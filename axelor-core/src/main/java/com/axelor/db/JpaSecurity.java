/*
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
package com.axelor.db;

import java.util.Set;

import com.axelor.i18n.I18n;
import com.axelor.rpc.filter.Filter;

public interface JpaSecurity {

	public static enum AccessType {

		READ	(/*$$(*/ "You are not authorized to read this resource." /*)*/),
		WRITE	(/*$$(*/ "You are not authorized to update this resource." /*)*/),
		CREATE	(/*$$(*/ "You are not authorized to create this resource." /*)*/),
		REMOVE	(/*$$(*/ "You are not authorized to remove this resource." /*)*/),
		EXPORT 	(/*$$(*/ "You are not authorized to export the data." /*)*/);

		private String message;
		private AccessType(String message) {
			this.message = message;
		}

		public String getMessage() {
			return I18n.get(message);
		}
	}

	public static final AccessType CAN_READ = AccessType.READ;
	public static final AccessType CAN_WRITE = AccessType.WRITE;
	public static final AccessType CAN_CREATE = AccessType.CREATE;
	public static final AccessType CAN_REMOVE = AccessType.REMOVE;
	public static final AccessType CAN_EXPORT = AccessType.EXPORT;

	Set<AccessType> getAccessTypes(Class<? extends Model> model, Long id);

	boolean hasRole(String name);

	Filter getFilter(AccessType type, Class<? extends Model> model, Long... ids);

	boolean isPermitted(AccessType type, Class<? extends Model> model, Long... ids);

	void check(AccessType type, Class<? extends Model> model, Long... ids);
}
