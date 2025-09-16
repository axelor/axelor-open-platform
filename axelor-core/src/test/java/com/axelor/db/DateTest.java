/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
