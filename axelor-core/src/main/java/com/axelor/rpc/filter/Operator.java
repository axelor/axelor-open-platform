package com.axelor.rpc.filter;

public enum Operator {

	AND("AND"),

	OR("OR"),

	NOT("NOT"),

	EQ("="),

	NEQ("!="),

	LT("<"),

	GT(">"),

	LTE("<="),

	GTE(">="),

	LIKE("LIKE"),

	NOT_LIKE("NOT LIKE"),

	IS_NULL("IS NULL"),

	NOT_NULL("IS NOT NULL"),

	IN("IN"),

	NO_IN("NOT IN"),

	BETWEEN("BETWEEN"),

	NOT_BETWEEN("NOT BETWEEN");

	private String value;

	private Operator(String value) {
		this.value = value;
	}

	public static Operator of(String value) {
		for (Operator op : Operator.values()) {
			if (op.value.equals(value))
				return op;
		}
		throw new IllegalArgumentException("No such operator: " + value);
	}

	@Override
	public String toString() {
		return value;
	}
}
