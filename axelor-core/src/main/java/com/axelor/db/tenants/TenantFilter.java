/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.db.tenants;

import com.axelor.common.StringUtils;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.pac4j.core.context.HttpConstants;

@Singleton
public class TenantFilter implements Filter {

  private static final String TENANT_COOKIE_NAME = "LAST-TENANT-ID";

  private static final String TENANT_ATTRIBUTE_NAME = "TENANT-ID";

  private static final String TENANT_HEADER_NAME = "X-Tenant-ID";

  private static final String PATH_CALLBACK = "/callback";

  private boolean enabled;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    enabled = TenantModule.isEnabled();
  }

  @Override
  public void destroy() {}

  @Override
  public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (enabled) {
      doFilterInternal(request, response, chain);
    } else {
      chain.doFilter(request, response);
    }
  }

  private void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    final HttpServletRequest req = (HttpServletRequest) request;
    final HttpServletResponse res = (HttpServletResponse) response;

    TenantResolver.CURRENT_HOST.set(req.getHeader("Host"));

    try {
      TenantResolver.CURRENT_TENANT.set(currentTenant(req, res));
      chain.doFilter(request, response);
    } catch (MissingTenantException e) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    } finally {
      TenantResolver.CURRENT_HOST.remove();
      TenantResolver.CURRENT_TENANT.remove();
    }
  }

  // When tenant comes from header/cookie, need to check it.
  // For login, consider header only.
  private Optional<String> getRequestTenant(HttpServletRequest req, boolean isLogin) {
    return Optional.ofNullable(req.getHeader(TENANT_HEADER_NAME))
        .filter(StringUtils::notBlank)
        .or(
            () ->
                isLogin
                    ? Optional.empty()
                    : Optional.ofNullable(getCookie(req, TENANT_COOKIE_NAME))
                        .map(Cookie::getValue)
                        .filter(StringUtils::notBlank))
        .filter(tenant -> TenantResolver.getTenants(false).containsKey(tenant));
  }

  private String currentTenant(HttpServletRequest req, HttpServletResponse res)
      throws MissingTenantException {
    final HttpSession httpSession = req.getSession(false);
    final Optional<String> sessionTenant =
        Optional.ofNullable(httpSession)
            .map(session -> (String) session.getAttribute(TENANT_ATTRIBUTE_NAME));
    final boolean isLogin = PATH_CALLBACK.equals(req.getServletPath());
    final String tenant =
        sessionTenant
            .or(() -> getRequestTenant(req, isLogin))
            .orElseGet(
                () -> {
                  final Map<String, String> tenants = TenantResolver.getTenants(false);
                  return tenants.size() == 1 ? tenants.keySet().iterator().next() : null;
                });

    if (tenant == null && (isLogin || req.getHeader(HttpConstants.AUTHORIZATION_HEADER) != null)) {
      throw new MissingTenantException();
    }

    if (isLogin) {
      setCookie(req, res, TENANT_COOKIE_NAME, tenant);
    }

    if (httpSession != null && sessionTenant.isEmpty() && tenant != null) {
      httpSession.setAttribute(TENANT_ATTRIBUTE_NAME, tenant);
    }

    return tenant;
  }

  private Cookie getCookie(HttpServletRequest request, String name) {
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

  private void setCookie(
      HttpServletRequest request, HttpServletResponse response, String name, String value) {
    Cookie cookie = getCookie(request, name);
    if (cookie == null) {
      cookie = new Cookie(name, value);
      if (request.isSecure()) {
        cookie.setSecure(true);
      }
    } else {
      cookie.setValue(value);
    }
    cookie.setHttpOnly(true);
    cookie.setMaxAge(60 * 60 * 24 * 7);
    response.addCookie(cookie);
  }
}
