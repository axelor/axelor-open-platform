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
package com.axelor.wkf.db.node.message;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.node.IntermediateMessageEvent;

@Entity
public class IntermediateMailEvent extends IntermediateMessageEvent {
	
	/**
	 * Find a <code>IntermediateMailEvent</code> by <code>id</code>.
	 *
	 */
	public static IntermediateMailEvent find(Long id) {
		return JPA.find(IntermediateMailEvent.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>IntermediateMailEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<IntermediateMailEvent> allIntermediateMailEvent() {
		return JPA.all(IntermediateMailEvent.class);
	}
	
	/**
	 * A shortcut method to <code>IntermediateMailEvent.all().filter(...)</code>
	 *
	 */
	public static Query<IntermediateMailEvent> filterIntermediateMailEvent(String filter, Object... params) {
		return JPA.all(IntermediateMailEvent.class).filter(filter, params);
	}
	
}
