/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.rpc.filter;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

class LogicalFilter extends Filter {

	private Operator operator;

	private List<Filter> filters;

	public LogicalFilter(Operator operator, List<Filter> filters) {
		this.operator = operator;
		this.filters = filters;
	}

	@Override
	public String getQuery() {

		if (filters == null || filters.size() == 0)
			return "";

		StringBuilder sb = new StringBuilder();
		
		if (operator == Operator.NOT)
			sb.append("NOT ");
		
		if (filters.size() > 1)
			sb.append("(");

		String joiner = operator == Operator.NOT ? " AND " : " "
				+ operator.name() + " ";
		sb.append(Joiner.on(joiner).join(filters));
		
		if (filters.size() > 1)
			sb.append(")");
		
		return sb.toString();
	}

	@Override
	public List<Object> getParams() {
		List<Object> params = new ArrayList<Object>();
		for (Filter filter : filters) {
			params.addAll(filter.getParams());
		}
		return params;
	}
}
