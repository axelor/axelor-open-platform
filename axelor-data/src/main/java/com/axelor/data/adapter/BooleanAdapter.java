package com.axelor.data.adapter;

import java.util.Map;
import java.util.regex.Pattern;

public class BooleanAdapter extends Adapter {

	private Pattern pattern;

	@Override
	public Object adapt(Object value, Map<String, Object> context) {
		
		if (value == null || "".equals(value)) {
			return Boolean.FALSE;
		}
		
		if (pattern == null) {
			String falsePattern = this.get("falsePattern", "(0|false|no|f|n)");
			pattern = Pattern.compile(falsePattern, Pattern.CASE_INSENSITIVE);
		}

		return !pattern.matcher((String) value).matches();
	}
}
