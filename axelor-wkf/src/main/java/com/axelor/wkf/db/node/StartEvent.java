/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.Node;

@Entity
public class StartEvent extends Node {
	
	/**
	 * Find a <code>StartEvent</code> by <code>id</code>.
	 *
	 */
	public static StartEvent find(Long id) {
		return JPA.find(StartEvent.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>StartEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<? extends StartEvent> all() {
		return JPA.all(StartEvent.class);
	}
	
	/**
	 * A shortcut method to <code>StartEvent.all().filter(...)</code>
	 *
	 */
	public static Query<? extends StartEvent> filter(String filter, Object... params) {
		return all().filter(filter, params);
	}
	
}
