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
	public static Query<Gateway> allGateway() {
		return JPA.all(Gateway.class);
	}
	
	/**
	 * A shortcut method to <code>Gateway.all().filter(...)</code>
	 *
	 */
	public static Query<Gateway> filterGateway(String filter, Object... params) {
		return JPA.all(Gateway.class).filter(filter, params);
	}
	
}
