/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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
import java.util.Enumeration;
import java.util.Map;

import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The {@link PreSessionTenantFilter} is responsible to set current tenant
 * before database connection is created.
 *
 */
@Singleton
public class PreSessionTenantFilter extends AbstractTenantFilter {

	private static final String LOGIN_PAGE = "/login.jsp";

	private boolean isLoginRequest(HttpServletRequest request) {
		return LOGIN_PAGE.equals(request.getServletPath());
	}

	private boolean isLoginSubmit(HttpServletRequest request) {
		return isLoginRequest(request) && "POST".equalsIgnoreCase(request.getMethod());
	}

	private static String getLoginParam(ServletRequest request, String param) {
		if (!isXHR(request)) {
			return request.getParameter(param);
		}
		try {
			return (String) new ObjectMapper().readValue(request.getInputStream(), Map.class).get(param);
		} catch (Exception e) {
			return null;
		}
	}

	private String currentTenant(ServletRequest request, ServletResponse response) {
		final HttpServletRequest req = (HttpServletRequest) request;
		final HttpServletResponse res = (HttpServletResponse) response;

		String tenantId = isLoginSubmit(req) ? getLoginParam(request, TENANT_LOGIN_PARAM) : null;
		Cookie tenantCookie = getCookie(req, TENANT_COOKIE_NAME);

		if (StringUtils.isBlank(tenantId) && tenantCookie != null) {
			tenantId = tenantCookie.getValue();
		}

		if (isLoginRequest(req)) {
			final HttpSession session = req.getSession();
			final Map<String, String> tenants = getTenants();
			final String switchTo = !isLoginSubmit(req) ? req.getParameter("tenant") : null;

			if (tenants.containsKey(switchTo)) {
				tenantId = switchTo;
			}

			if (!tenants.containsKey(tenantId)) {
				tenantId = tenants.isEmpty() ? null : tenants.keySet().iterator().next();
			}

			// update cookie on login attempt or change tenant request
			if (isLoginSubmit(req) || !StringUtils.isBlank(switchTo)) {
				if (switchTo != null) {
					// remove all session attribute except shiro attrs
					final Enumeration<String> attrs = session.getAttributeNames();
					while (attrs.hasMoreElements()) {
						final String attr = attrs.nextElement();
						if (!attr.startsWith(SESSION_KEY_PREFIX_SHIRO)) {
							session.removeAttribute(attr);
						}
					}
				}
				setCookie(req, res, TENANT_COOKIE_NAME, tenantId);
			}

			session.setAttribute(SESSION_KEY_TENANT_MAP, tenants);
			session.setAttribute(SESSION_KEY_TENANT_ID, tenantId);
		}

		return StringUtils.isBlank(tenantId) ? null : tenantId;
	}
	
	@Override
	protected boolean hasAccess(User user, TenantConfig config) {
		// there is no user at this stage, so show all available tenants
		return true;
	}

	@Override
	protected void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		final HttpServletRequest req = (HttpServletRequest) request;

		TenantResolver.CURRENT_HOST.set(req.getHeader("Host"));
		TenantResolver.CURRENT_TENANT.set(currentTenant(request, response));
		try {
			chain.doFilter(request, response);
		} finally {
			TenantResolver.CURRENT_HOST.remove();
			TenantResolver.CURRENT_TENANT.remove();
		}
	}

	@Override
	public void destroy() {
		TenantResolver.CURRENT_HOST.remove();
		TenantResolver.CURRENT_TENANT.remove();
	}
}
