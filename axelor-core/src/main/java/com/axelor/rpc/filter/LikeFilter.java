package com.axelor.rpc.filter;


class LikeFilter extends SimpleFilter {

	private LikeFilter(Operator operator, String fieldName, String value) {
		super(operator, fieldName, value);
	}

	private static String format(Object value) {
		String text = value.toString().toUpperCase();
		if (text.matches("(^%.*)|(.*%$)")) {
			return text;
		}
		return text = "%" + text + "%";
	}
	
	public static LikeFilter like(String fieldName, Object value) {
		return new LikeFilter(Operator.LIKE, fieldName, format(value));
	}
	
	public static LikeFilter notLike(String fieldName, Object value) {
		return new LikeFilter(Operator.NOT_LIKE, fieldName, format(value));
	}

	@Override
	public String getQuery() {
		return String.format("(UPPER(self.%s) %s ?)", getFieldName(), getOperator());
	}

}
