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
		throw new IllegalArgumentException(name);
	}

	@Override
	public String toString() {
		return value;
	}
}
