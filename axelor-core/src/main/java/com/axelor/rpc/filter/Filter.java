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
package com.axelor.rpc.filter;

import java.util.Collection;
import java.util.List;

import com.axelor.db.Model;
import com.axelor.db.Query;
import com.google.common.collect.Lists;

public abstract class Filter {

	private String query;

	public abstract String getQuery();

	public abstract List<Object> getParams();
	
	public <T extends Model> Query<T> build(Class<T> klass) {
		Query<T> query = Query.of(klass);
		StringBuilder sb = new StringBuilder(this.toString());
		int n = 0, i = sb.indexOf("?");
		while (i > -1) {
			sb.replace(i, i + 1, "?" + (++n));
			i = sb.indexOf("?", i + 1);
		}
		query.filter(sb.toString(), getParams().toArray());
		return query;
	}

	@Override
	public String toString() {
		if (query == null) {
			query = getQuery();
		}
		return query;
	}

	public static Filter equals(String fieldName, Object value) {
		return new SimpleFilter(Operator.EQUALS, fieldName, value);
	}

	public static Filter notEquals(String fieldName, Object value) {
		return new SimpleFilter(Operator.NOT_EQUAL, fieldName, value);
	}

	public static Filter lessThen(String fieldName, Object value) {
		return new SimpleFilter(Operator.LESS_THAN, fieldName, value);
	}

	public static Filter greaterThen(String fieldName, Object value) {
		return new SimpleFilter(Operator.GREATER_THAN, fieldName, value);
	}

	public static Filter lessOrEqual(String fieldName, Object value) {
		return new SimpleFilter(Operator.LESS_OR_EQUAL, fieldName, value);
	}

	public static Filter greaterOrEqual(String fieldName, Object value) {
		return new SimpleFilter(Operator.GREATER_OR_EQUAL, fieldName, value);
	}

	public static Filter like(String fieldName, Object value) {
		return LikeFilter.like(fieldName, value);
	}

	public static Filter notLike(String fieldName, Object value) {
		return LikeFilter.notLike(fieldName, value);
	}

	public static Filter isNull(String fieldName) {
		return NullFilter.isNull(fieldName);
	}

	public static Filter notNull(String fieldName) {
		return NullFilter.notNull(fieldName);
	}

	public static Filter in(String fieldName, Collection<?> value) {
		return new RangeFilter(Operator.IN, fieldName, value);
	}

	public static Filter in(String fieldName, Object first, Object second,
			Object... rest) {
		return in(fieldName, Lists.asList(first, second, rest));
	}

	public static Filter notIn(String fieldName, Collection<?> value) {
		return new RangeFilter(Operator.NOT_IN, fieldName, value);
	}

	public static Filter notIn(String fieldName, Object first, Object second,
			Object... rest) {
		return notIn(fieldName, Lists.asList(first, second, rest));
	}

	public static Filter between(String fieldName, Object start, Object end) {
		return new RangeFilter(Operator.BETWEEN, fieldName, Lists.newArrayList(
				start, end));
	}

	public static Filter notBetween(String fieldName, Object start, Object end) {
		return new RangeFilter(Operator.NOT_BETWEEN, fieldName,
				Lists.newArrayList(start, end));
	}

	public static Filter and(List<Filter> filters) {
		return new LogicalFilter(Operator.AND, filters);
	}

	public static Filter and(Filter first, Filter second, Filter... rest) {
		return and(Lists.asList(first, second, rest));
	}

	public static Filter or(List<Filter> filters) {
		return new LogicalFilter(Operator.OR, filters);
	}

	public static Filter or(Filter first, Filter second, Filter... rest) {
		return or(Lists.asList(first, second, rest));
	}

	public static Filter not(List<Filter> filters) {
		return new LogicalFilter(Operator.NOT, filters);
	}

	public static Filter not(Filter first, Filter second, Filter... rest) {
		return not(Lists.asList(first, second, rest));
	}
}
