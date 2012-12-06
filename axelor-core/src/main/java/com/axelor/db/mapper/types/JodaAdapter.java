package com.axelor.db.mapper.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
		if (value == null) {
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
			return new DateTime(value, DateTimeZone.UTC);
		}
	}

	class LocalDateAdapter implements TypeAdapter<LocalDate> {

		@Override
		public Object adapt(Object value, Class<?> actualType,
				Type genericType, Annotation[] annotations) {
			return new LocalDateTime(value, DateTimeZone.UTC).toLocalDate();
		}
	}

	class LocalTimeAdapter implements TypeAdapter<LocalTime> {

		@Override
		public Object adapt(Object value, Class<?> actualType,
				Type genericType, Annotation[] annotations) {
			try {
				return new DateTime(value, DateTimeZone.UTC).toLocalTime();
			} catch (IllegalArgumentException e) {
				DateTime dt = new DateTime();
				String val = String.format("%d-%d-%dT%s", dt.getYear(), dt.getMonthOfYear(), dt.getDayOfMonth(), value);
				return new DateTime(val, DateTimeZone.UTC).toLocalTime();
			}
		}
	}

	class LocalDateTimeAdapter implements TypeAdapter<LocalDateTime> {

		@Override
		public Object adapt(Object value, Class<?> actualType,
				Type genericType, Annotation[] annotations) {
			return new DateTime(value, DateTimeZone.UTC).toLocalDateTime();
		}
	}
}
