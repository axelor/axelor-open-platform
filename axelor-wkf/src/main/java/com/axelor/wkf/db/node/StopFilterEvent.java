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
public class StopFilterEvent extends Node {

	/**
	 * Find a <code>StopFilter</code> by <code>id</code>.
	 *
	 */
	public static StopFilterEvent find(Long id) {
		return JPA.find(StopFilterEvent.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>StopFilter</code> to StopFilter
	 * on all the records.
	 *
	 */
	public static Query<StopFilterEvent> allStopFilter() {
		return JPA.all(StopFilterEvent.class);
	}
	
	/**
	 * A shortcut method to <code>StopFilter.all().StopFilter(...)</code>
	 *
	 */
	public static Query<StopFilterEvent> StopFilterStopFilter(String StopFilter, Object... params) {
		return JPA.all(StopFilterEvent.class).filter(StopFilter, params);
	}

}
