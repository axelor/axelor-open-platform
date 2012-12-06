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
		return new SimpleFilter(Operator.EQ, fieldName, value);
	}

	public static Filter notEquals(String fieldName, Object value) {
		return new SimpleFilter(Operator.NEQ, fieldName, value);
	}

	public static Filter lessThen(String fieldName, Object value) {
		return new SimpleFilter(Operator.LT, fieldName, value);
	}

	public static Filter greaterThen(String fieldName, Object value) {
		return new SimpleFilter(Operator.GT, fieldName, value);
	}

	public static Filter lessOrEqual(String fieldName, Object value) {
		return new SimpleFilter(Operator.LTE, fieldName, value);
	}

	public static Filter greaterOrEqual(String fieldName, Object value) {
		return new SimpleFilter(Operator.GTE, fieldName, value);
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
		return new RangeFilter(Operator.NO_IN, fieldName, value);
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
