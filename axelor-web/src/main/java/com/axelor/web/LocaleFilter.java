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
package com.axelor.web;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.axelor.app.AppSettings;
import com.axelor.meta.service.MetaTranslations;
import com.google.inject.Singleton;

@Singleton
public class LocaleFilter implements Filter {

	private static ThreadLocal<String> BASE_URL;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@SuppressWarnings("all")
	private void setBaseUrl(ServletRequest req) {
		String baseUrl;
		if (req.getServerPort() == 80 ||
			req.getServerPort() == 443) {
			baseUrl = req.getScheme() + "://" + req.getServerName() + req.getServletContext().getContextPath();
		} else {
			baseUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getServletContext().getContextPath();
		}

		if (BASE_URL == null) {
			try {
				Field field = AppSettings.class.getDeclaredField("BASE_URL");
				field.setAccessible(true);
				BASE_URL = (ThreadLocal<String>) field.get(AppSettings.class);
			} catch (Exception e) {
			}
		}

		if (BASE_URL != null) {
			BASE_URL.set(baseUrl);
		}
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		setBaseUrl(request);
		MetaTranslations.language.set(request.getLocale());
		try {
			chain.doFilter(request, response);
		} finally {
			MetaTranslations.language.remove();
			if (BASE_URL != null) {
				BASE_URL.remove();
			}
		}
	}

	@Override
	public void destroy() {
		if (BASE_URL != null) {
			BASE_URL.remove();
			BASE_URL = null;
		}
	}
}
