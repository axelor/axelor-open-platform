package com.axelor.web;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * The {@link AppSessionListener} configures the session timeout.
 *
 */
@Singleton
public final class AppSessionListener implements HttpSessionListener {

	private int timeout;

	/**
	 * Create a new {@link AppSessionListener} with the given app settings.
	 *
	 * @param settings
	 *            application settings
	 */
	@Inject
	public AppSessionListener(AppSettings settings) {
		this.timeout = settings.getInt("session.timeout", 30);
	}

	@Override
	public void sessionCreated(HttpSessionEvent event) {
		event.getSession().setMaxInactiveInterval(timeout * 60);
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {

	}
}
