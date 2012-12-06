package com.axelor.rpc.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


class RangeFilter extends SimpleFilter {

	private Collection<?> values;

	public RangeFilter(Operator operator, String fieldName, Object value) {
		super(operator, fieldName, value);

		if (!(value instanceof Collection<?>)) {
			throw new IllegalArgumentException();
		}

		values = (Collection<?>) value;
	}

	@Override
	public String getQuery() {

		if (getOperator() == Operator.BETWEEN
				|| getOperator() == Operator.NOT_BETWEEN) {
			return String.format("(self.%s %s ? AND ?)", getFieldName(),
					getOperator());
		}

		StringBuilder sb = new StringBuilder("self.").append(getFieldName());
		sb.append(" ").append(getOperator()).append(" (");

		Iterator<?> iter = values.iterator();
		iter.next();
		sb.append("?");
		while (iter.hasNext()) {
			sb.append(", ").append("?");
			iter.next();
		}

		sb.append(")");
		return sb.toString();
	}

	@Override
	public List<Object> getParams() {
		return new ArrayList<Object>(values);
	}
}
