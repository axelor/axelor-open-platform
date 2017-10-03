/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
		
		String format = this.get("format", DEFAULT_FORMAT);

		DateTimeFormatter fmt = DateTimeFormat.forPattern(format);
		DateTime dt;
		try {
			dt = fmt.parseDateTime((String) value);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid value: " + value, e);
		}
		
		String type = this.get("type", null);

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
