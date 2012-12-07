package com.axelor.data.adapter;

import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class JodaAdapter extends Adapter {
	
	protected String DEFAULT_FORMAT = "yyyy-MM-ddTHH:mm:ss";

	@Override
	public Object adapt(Object value, Map<String, Object> context) {

		if (value == null || !(value instanceof String)) {
			return value;
		}
		
		String format = this.get("format");
		if (format == null) {
			format = DEFAULT_FORMAT;
		}

		DateTimeFormatter fmt = DateTimeFormat.forPattern(format);
		DateTime dt;
		try {
			dt = fmt.parseDateTime((String) value);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid value: " + value, e);
		}
		
		String type = this.get("type");

		if ("LocalDate".equals(type)) {
			return dt.toLocalDate();
		}
		if ("LocalTime".equals(type)) {
			return dt.toLocalTime();
		}
		if ("LocalDateTime".equals(type)) {
			return dt.toLocalDateTime();
		}
		return dt;
	}
}
