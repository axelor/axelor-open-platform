/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.rpc.filter;

import java.util.Arrays;
import java.util.List;

public class JPQLFilter extends Filter {

	private String jpql;

	private Object[] params;

	public JPQLFilter(String jpql, Object... params) {
		this.jpql = jpql;
		this.params = params;
	}

	@Override
	public String getQuery() {
		return "(" + this.jpql + ")";
	}

	@Override
	public List<Object> getParams() {
		return Arrays.asList(this.params);
	}
}
