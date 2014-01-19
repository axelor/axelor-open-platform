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
package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.Node;

@Entity
public class FilterEvent extends Node {
	
	private String filter;
	
	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	/**
	 * Find a <code>Filter</code> by <code>id</code>.
	 *
	 */
	public static FilterEvent find(Long id) {
		return JPA.find(FilterEvent.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>Filter</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<FilterEvent> allFilter() {
		return JPA.all(FilterEvent.class);
	}
	
	/**
	 * A shortcut method to <code>Filter.all().filter(...)</code>
	 *
	 */
	public static Query<FilterEvent> filterFilter(String filter, Object... params) {
		return JPA.all(FilterEvent.class).filter(filter, params);
	}

}
