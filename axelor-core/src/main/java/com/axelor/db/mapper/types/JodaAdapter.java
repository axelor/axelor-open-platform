/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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
package com.axelor.db.mapper.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import com.axelor.db.mapper.TypeAdapter;

public class JodaAdapter implements TypeAdapter<Object> {

	private DateTimeAdapter dateTimeAdapter = new DateTimeAdapter();
	private LocalDateAdapter localDateAdapter = new LocalDateAdapter();
	private LocalTimeAdapter localTimeAdapter = new LocalTimeAdapter();
	private LocalDateTimeAdapter localDateTimeAdapter = new LocalDateTimeAdapter();

	@Override
	public Object adapt(Object value, Class<?> actualType, Type genericType,
			Annotation[] annotations) {

		//TODO: check for annotation to return current date if value is null
		if (value == null || (value instanceof String && "".equals(((String) value).trim()))) {
			return null;
		}
		
		if (DateTime.class.isAssignableFrom(actualType))
			return dateTimeAdapter.adapt(value, actualType, genericType,
					annotations);

		else if (LocalDate.class.isAssignableFrom(actualType))
			return localDateAdapter.adapt(value, actualType, genericType,
					annotations);

		else if (LocalTime.class.isAssignableFrom(actualType))
			return localTimeAdapter.adapt(value, actualType, genericType,
					annotations);

		else if (LocalDateTime.class.isAssignableFrom(actualType))
			return localDateTimeAdapter.adapt(value, actualType, genericType,
					annotations);

		return value;
	}

	public boolean isJodaObject(Class<?> actualType) {
		return DateTime.class.isAssignableFrom(actualType)
				|| LocalDate.class.isAssignableFrom(actualType)
				|| LocalTime.class.isAssignableFrom(actualType)
				|| LocalDateTime.class.isAssignableFrom(actualType);
	}

	class DateTimeAdapter implements TypeAdapter<DateTime> {

		@Override
		public Object adapt(Object value, Class<?> actualType,
				Type genericType, Annotation[] annotations) {
			return new DateTime(value);
		}
	}

	class LocalDateAdapter implements TypeAdapter<LocalDate> {

		@Override
		public Object adapt(Object value, Class<?> actualType,
				Type genericType, Annotation[] annotations) {
			return new DateTime(value).toLocalDate();
		}
	}

	class LocalTimeAdapter implements TypeAdapter<LocalTime> {

		@Override
		public Object adapt(Object value, Class<?> actualType,
				Type genericType, Annotation[] annotations) {
			try {
				return new DateTime(value).toLocalTime();
			} catch (IllegalArgumentException e) {
				DateTime dt = new DateTime();
				String val = String.format("%d-%d-%dT%s", dt.getYear(), dt.getMonthOfYear(), dt.getDayOfMonth(), value);
				return new DateTime(val).toLocalTime();
			}
		}
	}

	class LocalDateTimeAdapter implements TypeAdapter<LocalDateTime> {

		@Override
		public Object adapt(Object value, Class<?> actualType,
				Type genericType, Annotation[] annotations) {
			return new DateTime(value).toLocalDateTime();
		}
	}
}
