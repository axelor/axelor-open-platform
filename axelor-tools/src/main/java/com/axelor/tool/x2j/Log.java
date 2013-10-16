/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.tool.x2j;

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
