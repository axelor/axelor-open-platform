/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.tenants;

import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.auth.pac4j.AxelorSessionManager;
import com.axelor.common.StringUtils;
import com.axelor.inject.Beans;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.shiro.session.Session;
import org.pac4j.core.context.HttpConstants;

@Singleton
public class TenantFilter implements Filter {

  private static final String TENANT_COOKIE_NAME = "LAST-TENANT-ID";

  private static final String TENANT_ATTRIBUTE_NAME = "TENANT-ID";

  private static final String TENANT_HEADER_NAME = "X-Tenant-ID";

  private static final String PATH_CALLBACK = "/callback";

  private boolean enabled;

  private final AxelorSessionManager sessionManager;

  private final TenantConfigProvider tenantConfigProvider;

  @Inject
  public TenantFilter(
      AxelorSessionManager sessionManager, TenantConfigProvider tenantConfigProvider) {
    this.sessionManager = sessionManager;
    this.tenantConfigProvider = tenantConfigProvider;
  }

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
    } catch (BadTenantException e) {

      // Only HTTP requests are intercepted in front-end.
      if (AuthPac4jInfo.isWebSocket(req)) {
        res.sendError(HttpServletResponse.SC_FORBIDDEN);
      } else {
        final Session session = sessionManager.getSession(req, res);

        if (session != null) {
          session.stop();
        }

        removeCookie(req, res, TENANT_COOKIE_NAME);

        if (AuthPac4jInfo.isXHR(req)) {
          // Ajax request
          res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          res.setContentType(HttpConstants.APPLICATION_JSON);
          response.setCharacterEncoding(StandardCharsets.UTF_8.name());
          final ObjectMapper mapper = Beans.get(ObjectMapper.class);
          res.getWriter()
              .write(mapper.writeValueAsString(Map.of("status", 1, "error", e.getMessage())));
        } else {
          // Full page load
          res.sendRedirect(".");
        }
      }
    } finally {
      TenantResolver.CURRENT_HOST.remove();
      TenantResolver.CURRENT_TENANT.remove();
    }
  }

  /**
   * Gets tenant from header/cookie. For login, consider header only.
   *
   * @param request
   * @param isLogin
   * @return tenant
   */
  private Optional<String> getRequestTenant(HttpServletRequest request, boolean isLogin) {
    return Optional.ofNullable(request.getHeader(TENANT_HEADER_NAME))
        .filter(StringUtils::notBlank)
        .or(
            () ->
                isLogin
                    ? Optional.empty()
                    : Optional.ofNullable(getCookie(request, TENANT_COOKIE_NAME))
                        .map(Cookie::getValue)
                        .filter(StringUtils::notBlank));
  }

  private String currentTenant(HttpServletRequest req, HttpServletResponse res) {
    final Session session = sessionManager.getSession(req, res);
    final Optional<String> sessionTenant =
        Optional.ofNullable(session).map(s -> (String) s.getAttribute(TENANT_ATTRIBUTE_NAME));
    final boolean isLogin = PATH_CALLBACK.equals(req.getServletPath());

    // Get tenant from session first, then request, then any host-resolved tenant.
    final String tenant =
        sessionTenant
            .filter(t -> !isLogin)
            .or(() -> getRequestTenant(req, isLogin))
            .orElseGet(() -> TenantResolver.getTenantInfo(false).getHostTenant());

    // Missing tenant
    if (tenant == null && (isLogin || req.getHeader(HttpConstants.AUTHORIZATION_HEADER) != null)) {
      throw new BadTenantException();
    }

    if (tenant != null) {
      final TenantConfig config = tenantConfigProvider.find(tenant);

      // Check active tenant
      if (config == null || Boolean.FALSE.equals(config.getActive())) {
        throw new TenantNotFoundException(tenant);
      }

      // Check tenant host
      final String hosts = config.getTenantHosts();
      if (StringUtils.notBlank(hosts)
          && !List.of(hosts.split("\\s*,\\s*")).contains(TenantResolver.CURRENT_HOST.get())) {
        throw new BadTenantException();
      }
    }

    if (session != null && sessionTenant.isEmpty()) {
      session.setAttribute(TENANT_ATTRIBUTE_NAME, tenant);
    }

    if (isLogin) {
      setCookie(req, res, TENANT_COOKIE_NAME, tenant);
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
    } else {
      cookie.setValue(value);
    }

    cookie.setHttpOnly(true);
    cookie.setMaxAge(60 * 60 * 24 * 7);

    if (request.isSecure()) {
      cookie.setSecure(true);
      cookie.setAttribute("SameSite", "None");
    }

    response.addCookie(cookie);
  }

  private void removeCookie(HttpServletRequest request, HttpServletResponse response, String name) {
    Cookie cookie = getCookie(request, name);
    if (cookie != null) {
      cookie.setMaxAge(0);
      cookie.setValue("");
      response.addCookie(cookie);
    }
  }
}
