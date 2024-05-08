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
import java.util.Objects;
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
import org.apache.shiro.web.util.WebUtils;

@Singleton
public class TenantFilter implements Filter {

  private static final String TENANT_COOKIE_NAME = "TENANT-ID";

  private static final String TENANT_HEADER_NAME = "X-Tenant-ID";

  private static final String TENANT_PARAM_NAME = "id";

  private static final String PATH_CALLBACK = "/callback";

  private static final String PATH_TENANT = "/tenant";

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
    TenantResolver.CURRENT_TENANT.set(currentTenant(req, res));
    try {
      chain.doFilter(request, response);
    } finally {
      TenantResolver.CURRENT_HOST.remove();
      TenantResolver.CURRENT_TENANT.remove();
    }
  }

  private String currentTenant(HttpServletRequest req, HttpServletResponse res) {
    final String tenant =
        Optional.ofNullable(req.getHeader(TENANT_HEADER_NAME))
            .filter(StringUtils::notBlank)
            .orElseGet(
                () ->
                    Optional.ofNullable(getCookie(req, TENANT_COOKIE_NAME))
                        .map(Cookie::getValue)
                        .filter(StringUtils::notBlank)
                        .orElse(null));

    final String target =
        isTenantRequest(req)
            ? Optional.ofNullable(req.getParameter(TENANT_PARAM_NAME))
                .filter(StringUtils::notBlank)
                .orElse(tenant)
            : tenant;

    final boolean switched = !Objects.equals(target, tenant);

    // is login or switch to another tenant
    if (isLoginSubmit(req) || switched) {
      setCookie(req, res, TENANT_COOKIE_NAME, target);
    }

    // redirect to home
    if (switched) {
      try {
        WebUtils.issueRedirect(req, res, "/");
      } catch (IOException e) {
      }
    }

    return target;
  }

  private boolean isTenantRequest(HttpServletRequest request) {
    return PATH_TENANT.equals(request.getServletPath());
  }

  private boolean isLoginSubmit(HttpServletRequest request) {
    return PATH_CALLBACK.equals(request.getServletPath())
        && "POST".equalsIgnoreCase(request.getMethod());
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
