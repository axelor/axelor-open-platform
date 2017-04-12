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
package com.axelor.common.logging;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

/**
 * Custom color converter for logback using jansi.
 *
 */
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
