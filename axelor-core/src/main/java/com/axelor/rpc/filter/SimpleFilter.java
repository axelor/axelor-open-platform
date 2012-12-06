package com.axelor.rpc.filter;

import java.util.ArrayList;
import java.util.List;


class SimpleFilter extends Filter {

	private String fieldName;

	private Operator operator;

	private Object value;

	public SimpleFilter(Operator operator, String fieldName, Object value) {
		this.fieldName = fieldName;
		this.operator = operator;
		this.value = value;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public Operator getOperator() {
		return operator;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	@Override
	public String getQuery() {
		return String.format("(self.%s %s ?)", fieldName, operator);
	}

	@Override
	public List<Object> getParams() {
		List<Object> params = new ArrayList<Object>();
		params.add(value);
		return params;
	}
}
