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
package com.axelor.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.axelor.app.AppSettings;

/**
 * Redirects static resource requests to it's minified version.
 *
 * <p>
 * For example, it can redirect <code>application.js</code> and
 * <code>application.css</code> to <code>application.min.js</code> and
 * <code>application.min.css</code> respectively in production mode.
 *
 */
@Singleton
public class MinifyFilter implements Filter {

	private static final Pattern pattern = Pattern.compile("(.*?)\\.(js|css)$");

	private boolean production;
	private String contextPath;
	private int contextLength;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		production = AppSettings.get().isProduction();
		contextPath = filterConfig.getServletContext().getContextPath();
		contextLength = contextPath.length();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		if (!production) {
			chain.doFilter(request, response);
			return;
		}

		final HttpServletRequest req = (HttpServletRequest) request;
		final HttpServletResponse res = (HttpServletResponse) response;

		final String path = req.getRequestURI();
		final Matcher matcher = pattern.matcher(path);

		if (!matcher.matches()) {
			chain.doFilter(request, response);
			return;
		}

		final String minified = matcher.group(1).substring(contextLength) + ".min." + matcher.group(2);

		if (exists(minified, req.getServletContext())) {
			res.sendRedirect(contextPath + minified);
			return;
		}

		chain.doFilter(request, response);
	}

	private boolean exists(String path, ServletContext context) {
		try {
			return context.getResource(path) != null;
		} catch (MalformedURLException e) {
			return false;
		}
	}

	@Override
	public void destroy() {
	}
}
