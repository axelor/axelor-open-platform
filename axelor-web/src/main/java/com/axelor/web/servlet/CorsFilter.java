/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.servlet;

import static com.axelor.common.StringUtils.isBlank;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_MAX_AGE;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static com.google.common.net.HttpHeaders.ORIGIN;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.HttpHeaders.HOST;
import static jakarta.ws.rs.core.HttpHeaders.VARY;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import jakarta.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple CORS filter that checks <code>Origin</code> header of the requests with the allowed
 * origins.
 *
 * <p>If the <code>Origin</code> header matches the configured allowed origins, it will set other
 * configured CORS headers.
 */
@Singleton
public class CorsFilter implements Filter {

  private static final String CONTENT_TYPE_JSON = MediaType.APPLICATION_JSON;

  private static final boolean DEFAULT_CORS_ALLOW_CREDENTIALS = false;
  private static final String DEFAULT_CORS_ALLOW_METHODS = "GET,PUT,POST,DELETE,HEAD,OPTIONS";
  private static final String DEFAULT_CORS_ALLOW_HEADERS =
      "Origin,Accept,Authorization,X-Requested-With,X-CSRF-Token,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers";
  private static final String DEFAULT_EXPOSE_HEADERS = "X-CSRF-Token";
  private static final String DEFAULT_CORS_MAX_AGE = "1728000";

  private boolean corsAllowCredentials;
  private String corsAllowMethods;
  private String corsAllowHeaders;
  private String corsExposeHeaders;
  private String corsMaxAge;

  private Set<String> allowedOrigins = Set.of();
  private boolean corsEnabled;
  private boolean anyOrigin;

  private static final Logger log = LoggerFactory.getLogger(CorsFilter.class);

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

    final AppSettings settings = AppSettings.get();

    configure(
        settings.get(AvailableAppSettings.CORS_ALLOW_ORIGIN),
        settings.getBoolean(
            AvailableAppSettings.CORS_ALLOW_CREDENTIALS, DEFAULT_CORS_ALLOW_CREDENTIALS),
        settings.get(AvailableAppSettings.CORS_ALLOW_METHODS, DEFAULT_CORS_ALLOW_METHODS),
        settings.get(AvailableAppSettings.CORS_ALLOW_HEADERS, DEFAULT_CORS_ALLOW_HEADERS),
        settings.get(AvailableAppSettings.CORS_EXPOSE_HEADERS, DEFAULT_EXPOSE_HEADERS),
        settings.get(AvailableAppSettings.CORS_MAX_AGE, DEFAULT_CORS_MAX_AGE));
  }

  Filter configure(
      String allowOrigin,
      boolean allowCredentials,
      String allowMethods,
      String allowHeaders,
      String exposeHeaders,
      String maxAge) {

    corsEnabled = !isBlank(allowOrigin);
    corsAllowCredentials = allowCredentials;
    corsAllowMethods = allowMethods;
    corsAllowHeaders = allowHeaders;
    corsExposeHeaders = exposeHeaders;
    corsMaxAge = maxAge;

    if (!corsEnabled) {
      allowedOrigins = Set.of();
      anyOrigin = false;
      return this;
    }

    log.debug("CORS origin: {}", allowOrigin);

    final Set<String> origins = new HashSet<>();

    for (String part : allowOrigin.split(",")) {
      String trimmed = part.trim();
      if (isBlank(trimmed)) {
        continue;
      }
      if ("*".equals(trimmed)) {
        origins.add("*");
      } else if ("null".equalsIgnoreCase(trimmed)) {
        log.warn("Ignoring CORS origin 'null': it cannot be listed explicitly");
      } else {
        String normalized = stripDefaultPort(normalizeOrigin(trimmed));
        if (isValidOrigin(normalized)) {
          origins.add(normalized);
        } else {
          log.warn("Ignoring invalid CORS origin '{}', expected scheme://host[:port]", trimmed);
        }
      }
    }

    allowedOrigins = Collections.unmodifiableSet(origins);
    anyOrigin = allowedOrigins.contains("*");

    if (allowedOrigins.isEmpty()) {
      log.warn("No valid CORS origin configured: all cross-origin requests will be rejected");
    }

    // This will be rejected instead of just warned in the next major version.
    if (anyOrigin && corsAllowCredentials) {
      log.warn(
          "CORS allow-credentials is set to true with wildcard origin '*'. "
              + "Credentials will be ignored; explicit origins must be configured.");
    }
    return this;
  }

  Filter configure(String allowOrigin, boolean allowCredentials) {
    return configure(
        allowOrigin,
        allowCredentials,
        DEFAULT_CORS_ALLOW_METHODS,
        DEFAULT_CORS_ALLOW_HEADERS,
        DEFAULT_EXPOSE_HEADERS,
        DEFAULT_CORS_MAX_AGE);
  }

  Filter configure(String allowOrigin) {
    return configure(allowOrigin, DEFAULT_CORS_ALLOW_CREDENTIALS);
  }

  @Override
  public void destroy() {}

  private static String normalizeOrigin(String origin) {
    if (origin == null) {
      return null;
    }
    String normalized = origin.trim();
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized.toLowerCase(Locale.ROOT);
  }

  // Browsers never send the default port in the Origin header
  private static String stripDefaultPort(String origin) {
    if (origin == null) {
      return null;
    }
    if ((origin.startsWith("https://") && origin.endsWith(":443"))
        || (origin.startsWith("http://") && origin.endsWith(":80"))) {
      return origin.substring(0, origin.lastIndexOf(':'));
    }
    return origin;
  }

  private static boolean isValidOrigin(String origin) {
    if (isBlank(origin)) {
      return false;
    }

    try {
      var uri = new URI(origin);
      return uri.getScheme() != null
          && uri.getHost() != null
          && uri.getRawUserInfo() == null
          && isBlank(uri.getRawPath())
          && uri.getRawQuery() == null
          && uri.getRawFragment() == null;
    } catch (URISyntaxException e) {
      return false;
    }
  }

  private boolean isCrossOrigin(String origin, String host) {
    return !isBlank(origin) && !origin.endsWith("//" + host);
  }

  private boolean isOriginAllowed(String origin) {
    return anyOrigin || allowedOrigins.contains(normalizeOrigin(origin));
  }

  private boolean isPreflight(HttpServletRequest req) {
    return HttpMethod.OPTIONS.equals(req.getMethod())
        && !isBlank(req.getHeader(ACCESS_CONTROL_REQUEST_METHOD));
  }

  private boolean isTextPlain(HttpServletRequest req) {
    final String contentType = req.getContentType();
    return contentType != null && MediaType.TEXT_PLAIN.equals(contentType.split(";")[0]);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (!corsEnabled) {
      chain.doFilter(request, response);
      return;
    }

    final HttpServletRequest req = (HttpServletRequest) request;
    final HttpServletResponse res = (HttpServletResponse) response;
    final String origin = req.getHeader(ORIGIN);
    final String host = req.getHeader(HOST);

    // Do not append "Vary: Origin" for wildcards
    if (!anyOrigin) {
      res.addHeader(VARY, ORIGIN);
    }

    if (!isCrossOrigin(origin, host)) {
      chain.doFilter(request, response);
      return;
    }
    if (!isOriginAllowed(origin)) {
      res.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    // Send a literal Access-Control-Allow-Origin: *
    // and never send Access-Control-Allow-Credentials for wildcards
    if (anyOrigin) {
      res.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    } else {
      res.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
      if (corsAllowCredentials) {
        res.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
      }
    }

    // Handle preflight request
    if (isPreflight(req)) {
      res.setHeader(ACCESS_CONTROL_ALLOW_METHODS, corsAllowMethods);
      res.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, corsAllowHeaders);
      res.setHeader(ACCESS_CONTROL_MAX_AGE, corsMaxAge);
      res.setStatus(HttpServletResponse.SC_OK);
      return;
    }

    if (!isBlank(corsExposeHeaders)) {
      res.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, corsExposeHeaders);
    }

    // Force "application/json" if content-type is "text/plain"
    final ServletRequest wrapper = isTextPlain(req) ? new JsonRequest(req) : req;

    chain.doFilter(wrapper, response);
  }

  // wrapper class to force content type to be "application/json"
  private static class JsonRequest extends HttpServletRequestWrapper {

    public JsonRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public String getContentType() {
      return CONTENT_TYPE_JSON;
    }

    @Override
    public String getHeader(String name) {
      return CONTENT_TYPE.equalsIgnoreCase(name) ? CONTENT_TYPE_JSON : super.getHeader(name);
    }
  }
}
