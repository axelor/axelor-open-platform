/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jline.jansi.Ansi;
import org.jline.jansi.Ansi.Attribute;
import org.jline.jansi.Ansi.Color;

/** Custom color converter for logback using jansi. */
public class ColorConverter extends CompositeConverter<ILoggingEvent> {

  private static final Map<String, Color> ELEMENTS;
  private static final Map<Integer, Color> LEVELS;

  static {
    final Map<String, Color> elements = new HashMap<>();
    elements.put("faint", Color.DEFAULT);
    elements.put("red", Color.RED);
    elements.put("green", Color.GREEN);
    elements.put("yellow", Color.YELLOW);
    elements.put("blue", Color.BLUE);
    elements.put("magenta", Color.MAGENTA);
    elements.put("cyan", Color.CYAN);
    ELEMENTS = Collections.unmodifiableMap(elements);

    final Map<Integer, Color> levels = new HashMap<>();
    levels.put(Level.ERROR_INTEGER, Color.RED);
    levels.put(Level.WARN_INTEGER, Color.YELLOW);
    LEVELS = Collections.unmodifiableMap(levels);
  }

  @Override
  protected String transform(ILoggingEvent event, String in) {
    if (System.console() == null) {
      return in;
    }

    Color color = ELEMENTS.get(getFirstOption());
    if (color == null) {
      color = LEVELS.get(event.getLevel().toInteger());
      color = color == null ? Color.GREEN : color;
    }

    final Ansi ansi = Ansi.ansi();
    if (color == Color.DEFAULT) {
      ansi.a(Attribute.INTENSITY_FAINT);
    }

    return ansi.fg(color).a(in).reset().toString();
  }
}
