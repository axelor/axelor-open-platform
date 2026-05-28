/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
        return fmt.parseBest((String) value, ZonedDateTime::from, LocalDateTime::from);
    }
  }
}
