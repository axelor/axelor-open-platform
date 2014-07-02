/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
package com.axelor.tools.x2j;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import com.google.common.collect.Lists;

public final class Log {

	private Logger logger;

	private List<Listener> listeners = Lists.newArrayList();

	public Log(Logger logger) {
		this.logger = logger;
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void info(String format, Object... arguments) {
		log(Type.INFO, format, arguments);
	}

	public void debug(String format, Object... arguments) {
		log(Type.DEBUG, format, arguments);
	}

	public void error(String format, Object... arguments) {
		log(Type.ERROR, format, arguments);
	}

	private void log(Type type, String format, Object... arguments) {
		FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
		String message = ft.getMessage();

		if (type == Type.INFO) {
			logger.info(message);
		}
		if (type == Type.ERROR) {
			logger.error(message);
		}
		if (type == Type.DEBUG) {
			logger.debug(message);
		}
		for (Listener listener : listeners) {
			listener.onLog(type, message, ft.getThrowable());
		}
	}

	public static enum Type {
		INFO,
		ERROR,
		DEBUG
	}

	public static interface Listener {

		void onLog(Type type, String message, Throwable error);
	}
}
