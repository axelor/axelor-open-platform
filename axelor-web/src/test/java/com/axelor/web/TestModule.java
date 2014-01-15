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
package com.axelor.web;

import java.util.Set;

import com.axelor.db.JpaModule;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.rpc.filter.Filter;
import com.google.inject.AbstractModule;

public class TestModule extends AbstractModule {

	public static class MySecurity implements JpaSecurity {

		@Override
		public Set<AccessType> getAccessTypes(Class<? extends Model> model, Long id) {
			return null;
		}

		@Override
		public boolean hasRole(String name) {
			return false;
		}

		@Override
		public Filter getFilter(AccessType type, Class<? extends Model> model, Long... ids) {
			return null;
		}

		@Override
		public boolean isPermitted(AccessType type, Class<? extends Model> model, Long... ids) {
			return true;
		}

		@Override
		public void check(AccessType type, Class<? extends Model> model, Long... ids) {
		
		}
	}

	@Override
	protected void configure() {
		
		// bind fake security implementation
		bind(JpaSecurity.class).to(MySecurity.class);
		
		// initialize JPA
		install(new JpaModule("testUnit", true, false));
	}
}
