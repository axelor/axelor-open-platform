/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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
package com.axelor.db;

import java.util.Set;

import com.axelor.rpc.filter.Filter;

public interface JpaSecurity {
	
	public static enum AccessType {
		
		READ	("You are not authorized to read this resource."),
		WRITE	("You are not authorized to update this resource."),
		CREATE	("You are not authorized to create this resource."),
		REMOVE	("You are not authorized to remove this resource."),
		EXPORT 	("You are not authorized to export the data.");

		private String message;
		private AccessType(String message) {
			this.message = JPA.translate(message);
		}
		
		public String getMessage() {
			return message;
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
