package com.axelor.data.adapter;

import java.util.Map;

public class NumberAdapter extends Adapter {

	private String decimalSeparator;
	private String thousandSeparator;

	@Override
	public Object adapt(Object value, Map<String, Object> context) {
		
		if (value == null || "".equals(value)) {
			return null;
		}
		
		if (!(value instanceof String)) {
			return value;
		}

		if (decimalSeparator == null) {
			decimalSeparator = this.get("decimalSeparator", ".");
			thousandSeparator = this.get("thousandSeparator", ",");
		}

		return ((String) value)
				.replace(thousandSeparator, "")
				.replace(decimalSeparator, ".");
	}
}
