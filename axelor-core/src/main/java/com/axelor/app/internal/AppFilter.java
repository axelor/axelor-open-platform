/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
package com.axelor.app.internal;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.google.inject.Singleton;

@Singleton
public class AppFilter implements Filter {

	private static final ThreadLocal<String> BASE_URL = new ThreadLocal<>();
	private static final ThreadLocal<Locale> LANGUAGE = new ThreadLocal<>();

	private static Locale APP_LOCALE;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		try {
			APP_LOCALE = new Locale(AppSettings.get().get("application.locale"));
		} catch (Exception e) {
		}
	}

	public static String getBaseURL() {
		return BASE_URL.get();
	}

	public static Locale getLocale() {
		User user = AuthUtils.getUser();
		if (user != null && user.getLanguage() != null) {
			return new Locale(user.getLanguage());
		}
		if (user != null && APP_LOCALE != null) {
			return APP_LOCALE;
		}
		if (LANGUAGE.get() == null) {
			return Locale.getDefault();
		}
		return LANGUAGE.get();
	}

	private String getBaseUrl(ServletRequest req) {
		if (req.getServerPort() == 80 ||
			req.getServerPort() == 443) {
			return req.getScheme() + "://" + req.getServerName() + req.getServletContext().getContextPath();
		}
		return req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getServletContext().getContextPath();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		BASE_URL.set(getBaseUrl(request));
		LANGUAGE.set(request.getLocale());
		try {
			chain.doFilter(request, response);
		} finally {
			LANGUAGE.remove();
			BASE_URL.remove();
		}
	}

	@Override
	public void destroy() {
		LANGUAGE.remove();
		BASE_URL.remove();
	}
}
