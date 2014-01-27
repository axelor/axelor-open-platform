/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
