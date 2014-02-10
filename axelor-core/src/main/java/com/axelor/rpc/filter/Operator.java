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
package com.axelor.rpc.filter;

import com.google.common.base.CaseFormat;

public enum Operator {

	AND("AND"),

	OR("OR"),

	NOT("NOT"),

	EQUALS("="),

	NOT_EQUAL("!="),

	LESS_THAN("<"),

	GREATER_THAN(">"),

	LESS_OR_EQUAL("<="),

	GREATER_OR_EQUAL(">="),

	LIKE("LIKE"),

	NOT_LIKE("NOT LIKE"),

	IS_NULL("IS NULL"),

	NOT_NULL("IS NOT NULL"),

	IN("IN"),

	NOT_IN("NOT IN"),

	BETWEEN("BETWEEN"),

	NOT_BETWEEN("NOT BETWEEN");

	private String value;
	
	private String id;

	private Operator(String value) {
		this.value = value;
		this.id = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, this.name());
	}

	public static Operator get(String name) {
		for (Operator operator : Operator.values()) {
			if (operator.value.equals(name) || operator.id.equals(name)) {
				return operator;
			}
		}
		throw new IllegalArgumentException("No such operator: " + name);
	}

	@Override
	public String toString() {
		return value;
	}
}
