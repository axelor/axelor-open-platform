package com.axelor.rpc.filter;


class LikeFilter extends SimpleFilter {

	private LikeFilter(Operator operator, String fieldName, Object value) {
		super(operator, fieldName, value.toString().toUpperCase());
	}
	
	public static LikeFilter like(String fieldName, Object value) {
		return new LikeFilter(Operator.LIKE, fieldName, value);
	}
	
	public static LikeFilter notLike(String fieldName, Object value) {
		return new LikeFilter(Operator.NOT_LIKE, fieldName, value);
	}

	@Override
	public String getQuery() {
		return String.format("(UPPER(self.%s) %s ?)", getFieldName(), getOperator());
	}

}
