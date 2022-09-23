/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.axelor.db.mapper.Adapter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
class DateTest {

  private final TimeZone defaultTimeZone = TimeZone.getDefault();
  private final Collection<ZoneId> zoneIds = Set.of(ZoneOffset.UTC, ZoneOffset.MIN, ZoneOffset.MAX);

  @Test
  void testLocalDate() {
    final LocalDate expected = LocalDate.now();
    runInTimeZones(
        () -> {
          {
            final Object actual = Adapter.adapt(null, LocalDate.class, null, null);
            assertNull(actual);
          }
          {
            final String value = expected.toString();
            final Object actual = Adapter.adapt(value, LocalDate.class, null, null);
            assertEquals(expected, actual);
          }
          {
            final LocalDate value = expected;
            final Object actual = Adapter.adapt(value, LocalDate.class, null, null);
            assertEquals(expected, actual);
          }
          {
            final LocalDateTime value = expected.atStartOfDay();
            final Object actual = Adapter.adapt(value, LocalDate.class, null, null);
            assertEquals(expected, actual);
          }
          {
            final ZonedDateTime value = expected.atStartOfDay().atZone(ZoneId.systemDefault());
            final Object actual = Adapter.adapt(value, LocalDate.class, null, null);
            assertEquals(expected, actual);
          }
          {
            final Instant value =
                expected.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            final Object actual = Adapter.adapt(value, LocalDate.class, null, null);
            assertEquals(expected, actual);
          }
          {
            final Date value =
                Date.from(expected.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            final Object actual = Adapter.adapt(value, LocalDate.class, null, null);
            assertEquals(expected, actual);
          }
        });
  }

  @Test
  void testLocalDateTime() {
    final LocalDateTime expected = LocalDate.now().atTime(LocalTime.NOON);
    runInTimeZones(
        () -> {
          {
            final Object actual = Adapter.adapt(null, LocalDateTime.class, null, null);
            assertNull(actual);
          }
          {
            final String value = expected.toString();
            final Object actual = Adapter.adapt(value, LocalDateTime.class, null, null);
            assertEquals(expected, actual);
          }
          {
            final LocalDateTime value = expected;
            final Object actual = Adapter.adapt(value, LocalDateTime.class, null, null);
            assertEquals(expected, actual);
          }
          {
            final ZonedDateTime value = expected.atZone(ZoneId.systemDefault());
            final Object actual = Adapter.adapt(value, LocalDateTime.class, null, null);
            assertEquals(expected, actual);
          }
          {
            final Instant value = expected.atZone(ZoneId.systemDefault()).toInstant();
            final Object actual = Adapter.adapt(value, LocalDateTime.class, null, null);
            assertEquals(expected, actual);
          }
          {
            final Date value = Date.from(expected.atZone(ZoneId.systemDefault()).toInstant());
            final LocalDateTime actual =
                (LocalDateTime) Adapter.adapt(value, LocalDateTime.class, null, null);
            assertEquals(expected.getYear(), actual.getYear());
            assertEquals(expected.getMonth(), actual.getMonth());
            assertEquals(expected.getDayOfMonth(), actual.getDayOfMonth());
            assertEquals(expected.getHour(), actual.getHour());
            assertEquals(expected.getMinute(), actual.getMinute());
            assertEquals(expected.getSecond(), actual.getSecond());
          }
        });
  }

  @Test
  void testZonedDateTime() {
    final ZonedDateTime expected =
        LocalDate.now().atTime(LocalTime.NOON).atZone(ZoneId.systemDefault());
    runInTimeZones(
        () -> {
          {
            final Object actual = Adapter.adapt(null, ZonedDateTime.class, null, null);
            assertNull(actual);
          }
          {
            final String value = expected.toString();
            final Object actual = Adapter.adapt(value, ZonedDateTime.class, null, null);
            assertEquals(expected, actual);
          }
          {
            final LocalDateTime value = expected.toLocalDateTime();
            final ZonedDateTime actual =
                (ZonedDateTime) Adapter.adapt(value, ZonedDateTime.class, null, null);
            assertEquals(expected.toLocalDateTime(), actual.toLocalDateTime());
          }
          {
            final ZonedDateTime value = expected;
            final Object actual = Adapter.adapt(value, ZonedDateTime.class, null, null);
            assertEquals(expected, actual);
          }
          {
            final Instant value = expected.toInstant();
            final ZonedDateTime actual =
                (ZonedDateTime) Adapter.adapt(value, ZonedDateTime.class, null, null);
            assertEquals(expected.toInstant(), actual.toInstant());
          }
          {
            final Date value = Date.from(expected.toInstant());
            final ZonedDateTime actual =
                (ZonedDateTime) Adapter.adapt(value, ZonedDateTime.class, null, null);
            final var actualDate = Date.from(actual.toInstant());
            assertEquals(value, actualDate);
          }
        });
  }

  private void runInTimeZones(Runnable task) {
    try {
      for (final ZoneId zoneId : zoneIds) {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
        task.run();
      }
    } finally {
      TimeZone.setDefault(defaultTimeZone);
    }
  }
}
