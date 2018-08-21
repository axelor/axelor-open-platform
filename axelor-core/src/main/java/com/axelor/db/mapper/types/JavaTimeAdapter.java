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
package com.axelor.db.mapper.types;

import com.axelor.db.mapper.TypeAdapter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Date;

public class JavaTimeAdapter implements TypeAdapter<Object> {

  private DateTimeAdapter dateTimeAdapter = new DateTimeAdapter();
  private LocalDateAdapter localDateAdapter = new LocalDateAdapter();
  private LocalTimeAdapter localTimeAdapter = new LocalTimeAdapter();
  private LocalDateTimeAdapter localDateTimeAdapter = new LocalDateTimeAdapter();

  @Override
  public Object adapt(
      Object value, Class<?> actualType, Type genericType, Annotation[] annotations) {

    // TODO: check for annotation to return current date if value is null
    if (value == null || (value instanceof String && "".equals(((String) value).trim()))) {
      return null;
    }

    if (ZonedDateTime.class.isAssignableFrom(actualType))
      return dateTimeAdapter.adapt(value, actualType, genericType, annotations);

    if (LocalDate.class.isAssignableFrom(actualType))
      return localDateAdapter.adapt(value, actualType, genericType, annotations);

    if (LocalTime.class.isAssignableFrom(actualType))
      return localTimeAdapter.adapt(value, actualType, genericType, annotations);

    if (LocalDateTime.class.isAssignableFrom(actualType))
      return localDateTimeAdapter.adapt(value, actualType, genericType, annotations);

    return value;
  }

  public boolean isJavaTimeObject(Class<?> actualType) {
    return ZonedDateTime.class.isAssignableFrom(actualType)
        || LocalDate.class.isAssignableFrom(actualType)
        || LocalTime.class.isAssignableFrom(actualType)
        || LocalDateTime.class.isAssignableFrom(actualType);
  }

  private ZonedDateTime toZonedDateTime(Object value) {
    if (value == null) {
      return ZonedDateTime.now();
    }
    if (value instanceof Temporal) {
      return ZonedDateTime.from((Temporal) value);
    }
    if (value instanceof Date) {
      return ((Date) value).toInstant().atZone(ZoneId.systemDefault());
    }
    if (value instanceof Calendar) {
      return ((Calendar) value).toInstant().atZone(ZoneId.systemDefault());
    }
    try {
      return OffsetDateTime.parse(value.toString()).atZoneSameInstant(ZoneId.systemDefault());
    } catch (Exception e) {
    }
    try {
      return LocalDateTime.parse(value.toString()).atZone(ZoneId.systemDefault());
    } catch (Exception e) {
    }
    try {
      return LocalDate.parse(value.toString()).atStartOfDay().atZone(ZoneId.systemDefault());
    } catch (Exception e) {
    }
    throw new IllegalArgumentException("Unable to convert value: " + value);
  }

  class DateTimeAdapter implements TypeAdapter<ZonedDateTime> {

    @Override
    public Object adapt(
        Object value, Class<?> actualType, Type genericType, Annotation[] annotations) {
      return value instanceof ZonedDateTime ? value : toZonedDateTime(value);
    }
  }

  class LocalDateAdapter implements TypeAdapter<LocalDate> {

    @Override
    public Object adapt(
        Object value, Class<?> actualType, Type genericType, Annotation[] annotations) {
      return value instanceof LocalDate ? value : toZonedDateTime(value).toLocalDate();
    }
  }

  class LocalTimeAdapter implements TypeAdapter<LocalTime> {

    @Override
    public Object adapt(
        Object value, Class<?> actualType, Type genericType, Annotation[] annotations) {
      try {
        return value instanceof LocalTime ? value : toZonedDateTime(value).toLocalTime();
      } catch (Exception e) {
        final ZonedDateTime dt = ZonedDateTime.now();
        final String val =
            String.format(
                "%d-%02d-%02dT%s", dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth(), value);
        return toZonedDateTime(val).toLocalTime();
      }
    }
  }

  class LocalDateTimeAdapter implements TypeAdapter<LocalDateTime> {

    @Override
    public Object adapt(
        Object value, Class<?> actualType, Type genericType, Annotation[] annotations) {
      return value instanceof LocalDateTime ? value : toZonedDateTime(value).toLocalDateTime();
    }
  }
}
