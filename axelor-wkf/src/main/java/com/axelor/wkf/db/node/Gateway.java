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
import com.axelor.db.annotations.Widget;
import com.axelor.wkf.db.Node;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@Entity
public class Gateway extends Node {

	@Widget(selection = "node.logic.operator.selection")
	private String operator;

	public String getOperator() {
		return operator;
	}

	public void setOperator(String logicOperator) {
		this.operator = logicOperator;
	}

	@Override
	public String toString() {
		ToStringHelper tsh = Objects.toStringHelper(this);

		tsh.add("id", this.getId());
		tsh.add("name", this.getName());
		tsh.add("type", this.getType());
		tsh.add("operator", this.getOperator());
		tsh.add("ref", this.getRef());

		return tsh.omitNullValues().toString();
	}

	/**
	 * Find a <code>Gateway</code> by <code>id</code>.
	 *
	 */
	public static Gateway find(Long id) {
		return JPA.find(Gateway.class, id);
	}

	/**
	 * Return a {@link Query} instance for <code>Gateway</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<? extends Gateway> all() {
		return JPA.all(Gateway.class);
	}

	/**
	 * A shortcut method to <code>Gateway.all().filter(...)</code>
	 *
	 */
	public static Query<? extends Gateway> filter(String filter, Object... params) {
		return all().filter(filter, params);
	}

}
