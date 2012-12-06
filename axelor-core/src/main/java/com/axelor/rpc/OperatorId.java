package com.axelor.rpc;

public enum OperatorId {

	/**
	 * exactly equal to
	 */
	EQUALS("equals"),

	/**
	 * not equal to
	 */
	NOT_EQUAL("notEqual"),
	/**
	 * exactly equal to, if case is disregarded
	 */
	IEQUALS("iEquals"),
	/**
	 * not equal to, if case is disregarded
	 */
	INOT_EQUAL("iNotEqual"),
	/**
	 * Greater than
	 */
	GREATER_THAN("greaterThan"),
	/**
	 * Less than
	 */
	LESS_THAN("lessThan"),
	/**
	 * Greater than or equal to
	 */
	GREATER_OR_EQUAL("greaterOrEqual"),
	/**
	 * Less than or equal to
	 */
	LESS_OR_EQUAL("lessOrEqual"),
	/**
	 * Contains as sub-string (match case)
	 */
	CONTAINS("contains"),
	/**
	 * Starts with (match case)
	 */
	STARTS_WITH("startsWith"),
	/**
	 * Ends with (match case)
	 */
	ENDS_WITH("endsWith"),
	/**
	 * Contains as sub-string (case insensitive)
	 */
	ICONTAINS("iContains"),

	/**
	 * Starts with (case insensitive)
	 */
	ISTARTS_WITH("iStartsWith"),
	/**
	 * Ends with (case insensitive)
	 */
	IENDS_WITH("iEndsWith"),
	/**
	 * Does not contain as sub-string (match case)
	 */
	NOT_CONTAINS("notContains"),
	/**
	 * Does not start with (match case)
	 */
	NOT_STARTS_WITH("notStartsWith"),
	/**
	 * Does not end with (match case)
	 */
	NOT_ENDS_WITH("notEndsWith"),
	/**
	 * Does not contain as sub-string (case insensitive)
	 */
	INOT_CONTAINS("iNotContains"),
	/**
	 * Does not start with (case insensitive)
	 */
	INOT_STARTS_WITH("iNotStartsWith"),
	/**
	 * Does not end with (case insensitive)
	 */
	INOT_ENDS_WITH("iNotEndsWith"),
	/**
	 * Regular expression match
	 */
	REGEXP("regexp"),
	/**
	 * Regular expression match (case insensitive)
	 */
	IREGEXP("iregexp"),
	/**
	 * value is null
	 */
	IS_NULL("isNull"),
	/**
	 * value is non-null. Note empty string ("") is non-null
	 */
	NOT_NULL("notNull"),
	/**
	 * value is in a set of values. Specify criterion.value as an Array
	 */
	IN_SET("inSet"),
	/**
	 * value is not in a set of values. Specify criterion.value as an Array
	 */
	NOT_IN_SET("notInSet"),
	/**
	 * matches another field (specify fieldName as criterion.value)
	 */
	EQUALS_FIELD("equalsField"),
	/**
	 * does not match another field (specify fieldName as criterion.value)
	 */
	NOT_EQUAL_FIELD("notEqualField"),
	/**
	 * Greater than another field (specify fieldName as criterion.value)
	 */
	GREATER_THAN_FIELD("greaterThanField"),
	/**
	 * Less than another field (specify fieldName as criterion.value)
	 */
	LESS_THAN_FIELD("lessThanField"),
	/**
	 * Greater than or equal to another field (specify fieldName as
	 * criterion.value)
	 */
	GREATER_OR_EQUAL_FIELD("greaterOrEqualField"),
	/**
	 * Less than or equal to another field (specify fieldName as
	 * criterion.value)
	 */
	LESS_OR_EQUAL_FIELD("lessOrEqualField"),
	/**
	 * Contains as sub-string (match case) another field value (specify
	 * fieldName as criterion.value)
	 */
	CONTAINS_FIELD("containsField"),
	/**
	 * Starts with (match case) another field value (specify fieldName as
	 * criterion.value)
	 */
	STARTS_WITH_FIELD("startsWithField"),
	/**
	 * Ends with (match case) another field value (specify fieldName as
	 * criterion.value)
	 */
	ENDS_WITH_FIELD("endsWithField"),
	/**
	 * all subcriteria (criterion.criteria) are true
	 */
	AND("and"),
	/**
	 * all subcriteria (criterion.criteria) are false
	 */
	NOT("not"),
	/**
	 * at least one subcriteria (criterion.criteria) is true
	 */
	OR("or"),
	/**
	 * shortcut for greaterThan + lessThan + and. Specify criterion.start and
	 * criterion.end
	 */
	BETWEEN("between"),
	/**
	 * shortcut for greaterOrEqual + lessOrEqual + and. Specify criterion.start
	 * and criterion.end
	 */
	BETWEEN_INCLUSIVE("betweenInclusive");

	private String value;

	private OperatorId(String value) {
		this.value = value;
	}

	public static OperatorId get(String value) {

		for (OperatorId op : OperatorId.values()) {
			if (op.value.equals(value))
				return op;
		}
		throw new IllegalArgumentException(value);
	}
}
