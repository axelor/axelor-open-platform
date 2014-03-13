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
package com.axelor.wkf.db.node.message.socialnetwork;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.node.message.IntermediateSocialNetworkEvent;

@Entity
public class IntermediateTwitterEvent extends IntermediateSocialNetworkEvent {

	/**
	 * Find a <code>IntermediateTwitterEvent</code> by <code>id</code>.
	 *
	 */
	public static IntermediateTwitterEvent find(Long id) {
		return JPA.find(IntermediateTwitterEvent.class, id);
	}

	/**
	 * Return a {@link Query} instance for <code>IntermediateTwitterEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<IntermediateTwitterEvent> allIntermediateTwitterEvent() {
		return JPA.all(IntermediateTwitterEvent.class);
	}
	
	/**
	 * A shortcut method to <code>IntermediateTwitterEvent.all().filter(...)</code>
	 *
	 */
	public static Query<IntermediateTwitterEvent> filterIntermediateTwitterEvent(String filter, Object... params) {
		return JPA.all(IntermediateTwitterEvent.class).filter(filter, params);
	}
	
}
