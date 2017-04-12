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
package com.axelor.db.tenants;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;

/**
 * The {@link AbstractTenantFilter} provides common code for all tenant filters.
 *
 */
public abstract class AbstractTenantFilter implements Filter {

	protected static final String TENANT_COOKIE_NAME = "TENANTID";
	protected static final String TENANT_LOGIN_PARAM = "tenantId";

	protected static final String SESSION_KEY_TENANT_MAP = "tenantMap";
	protected static final String SESSION_KEY_TENANT_ID = TENANT_LOGIN_PARAM;

	protected static final String SESSION_KEY_PREFIX_SHIRO = "org.apache.shiro";

	private boolean enabled;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		enabled = AppSettings.get().getBoolean(TenantModule.CONFIG_MULTI_TENANCY, false);
	}

	@Override
	public void destroy() {
	}

	@Override
	public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (enabled) {
			doFilterInternal(request, response, chain);
		} else {
			chain.doFilter(request, response);
		}
	}

	protected abstract void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException;

	protected boolean hasAccess(User user, TenantConfig config) {
		final TenantConfigProvider provider = TenantSupport.get().getConfigProvider();
		return provider.hasAccess(user, config);
	}

	protected Map<String, String> getTenants() {
		final TenantConfigProvider provider = TenantSupport.get().getConfigProvider();
		final Map<String, String> map = new LinkedHashMap<>();
		String first = null;
		for (TenantConfig config : provider.findAll(TenantResolver.CURRENT_HOST.get())) {
			if (config.getActive() == Boolean.FALSE || config.getVisible() == Boolean.FALSE
					|| !hasAccess(AuthUtils.getUser(), config)) {
				continue;
			}
			if (first == null) {
				first = config.getTenantId();
			}
			map.put(config.getTenantId(), config.getTenantName());
		}
		return map;
	}

	protected Cookie getCookie(HttpServletRequest request, String name) {
		final Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (name.equals(cookie.getName())) {
					return cookie;
				}
			}
		}
		return null;
	}

	protected void setCookie(HttpServletRequest request, HttpServletResponse response, String name, String value) {
		Cookie cookie = getCookie(request, name);
		if (cookie == null) {
			cookie = new Cookie(name, value);
		} else {
			cookie.setValue(value);
		}
		cookie.setHttpOnly(true);
		cookie.setMaxAge(60 * 60 * 24 * 7);
		response.addCookie(cookie);
	}

	protected static boolean isXHR(ServletRequest request) {
		final HttpServletRequest req = (HttpServletRequest) request;
		return "XMLHttpRequest".equals(req.getHeader("X-Requested-With"))
				|| "application/json".equals(req.getHeader("Accept"))
				|| "application/json".equals(req.getHeader("Content-Type"));
	}
}
