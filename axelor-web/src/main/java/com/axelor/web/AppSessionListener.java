/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
package com.axelor.web;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import net.sf.ehcache.util.concurrent.ConcurrentHashMap;

import com.axelor.app.AppSettings;

/**
 * The {@link AppSessionListener} configures the session timeout.
 *
 */
@Singleton
public final class AppSessionListener implements HttpSessionListener {

	private final int timeout;

	private static final Map<String, HttpSession> sessions = new ConcurrentHashMap<>();

	/**
	 * Create a new {@link AppSessionListener} with the given app settings.
	 *
	 * @param settings
	 *            application settings
	 */
	@Inject
	public AppSessionListener(AppSettings settings) {
		this.timeout = settings.getInt("session.timeout", 60);
	}

	@Override
	public void sessionCreated(HttpSessionEvent event) {
		final HttpSession session = event.getSession();
		sessions.put(session.getId(), session);
		session.setMaxInactiveInterval(timeout * 60);
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		final HttpSession session = event.getSession();
		sessions.remove(session.getId());
	}

	public static Set<String> getActiveSessions() {
		return sessions.keySet();
	}

	public static HttpSession getSession(String id) {
		return sessions.get(id);
	}
}
