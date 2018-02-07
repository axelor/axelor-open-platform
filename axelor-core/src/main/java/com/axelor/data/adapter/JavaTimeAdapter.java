/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class JavaTimeAdapter extends Adapter {
	
	protected String DEFAULT_FORMAT = "yyyy-MM-ddTHH:mm:ss";

	@Override
	public Object adapt(Object value, Map<String, Object> context) {
		if (value == null || !(value instanceof String)) {
			return value;
		}

		final String type = this.get("type", null);
		final String text = (String) value;

		final String format = this.get("format", DEFAULT_FORMAT);
		final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(format);

		switch (type) {
		case "LocalDate":
			return fmt.parse(text, LocalDate::from);
		case "LocalTime":
			return fmt.parse(text, LocalTime::from);
		case "LocalDateTime":
			return fmt.parse(text, LocalDateTime::from);
		default:
			return fmt.parse((String) value, ZonedDateTime::from);
		}
	}
}
