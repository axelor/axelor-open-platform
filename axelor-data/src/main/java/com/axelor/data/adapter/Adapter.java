package com.axelor.data.adapter;

import java.util.Map;
import java.util.Properties;

public abstract class Adapter {
	
	private Properties options;
	
	public void setOptions(Properties options) {
		this.options = options;
	}

	public String get(String option) {
		if (options == null) {
			return null;
		}
		return options.getProperty(option);
	}

	public abstract Object adapt(Object value, Map<String, Object> context);
}
