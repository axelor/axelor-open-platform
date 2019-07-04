/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.web.servlet;

import static com.axelor.common.StringUtils.isBlank;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple CORS filter that checks <code>Origin</code> header of the requests with the allowed origin
 * pattern.
 *
 * <p>If the <code>Origin</code> header matches the configured allowed pattern, it will set other
 * configured CORS headers.
 */
@Singleton
public class CorsFilter implements Filter {

  private static final String CONTENT_TYPE_JSON = "application/json";

  private static final String DEFAULT_CORS_ALLOW_ORIGIN = "*";
  private static final String DEFAULT_CORS_ALLOW_CREDENTIALS = "true";
  private static final String DEFAULT_CORS_ALLOW_METHODS = "GET,PUT,POST,DELETE,HEAD,OPTIONS";
  private static final String DEFAULT_CORS_ALLOW_HEADERS =
      "Origin,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers";
  private static final String DEFAULT_EXPOSE_HEADERS = "";
  private static final String DEFAULT_CORS_MAX_AGE = "1728000";

  private static Pattern corsOriginPattern;

  private static String corsAllowOrigin;
  private static String corsAllowCredentials;
  private static String corsAllowMethods;
  private static String corsAllowHeaders;
  private static String corsExposeHeaders;
  private static String corsMaxAge;

  private Logger log = LoggerFactory.getLogger(CorsFilter.class);

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

    final AppSettings settings = AppSettings.get();

    corsAllowOrigin = settings.get(AvailableAppSettings.CORS_ALLOW_ORIGIN);
    corsAllowCredentials =
        settings.get(AvailableAppSettings.CORS_ALLOW_CREDENTIALS, DEFAULT_CORS_ALLOW_CREDENTIALS);
    corsAllowMethods =
        settings.get(AvailableAppSettings.CORS_ALLOW_METHODS, DEFAULT_CORS_ALLOW_METHODS);
    corsAllowHeaders =
        settings.get(AvailableAppSettings.CORS_ALLOW_HEADERS, DEFAULT_CORS_ALLOW_HEADERS);
    corsExposeHeaders =
        settings.get(AvailableAppSettings.CORS_EXPOSE_HEADERS, DEFAULT_EXPOSE_HEADERS);
    corsMaxAge = settings.get(AvailableAppSettings.CORS_MAX_AGE, DEFAULT_CORS_MAX_AGE);

    if (isBlank(corsAllowOrigin)) {
      return;
    }

    log.debug("CORS origin: {}", corsAllowOrigin);

    if (DEFAULT_CORS_ALLOW_ORIGIN.equals(corsAllowOrigin)) {
      corsOriginPattern = Pattern.compile(".*");
      return;
    }
    try {
      corsOriginPattern = Pattern.compile(corsAllowOrigin);
    } catch (PatternSyntaxException e) {
      log.error("CORS origin pattern is invalid", e);
      corsAllowOrigin = null;
    }
  }

  @Override
  public void destroy() {}

  private boolean isCrossOrigin(String origin, String host) {
    return !isBlank(origin) && !origin.endsWith("//" + host);
  }

  private boolean isOriginAllowed(String origin) {
    return DEFAULT_CORS_ALLOW_ORIGIN.equals(corsAllowOrigin)
        || corsOriginPattern.matcher(origin).matches();
  }

  private boolean isPreflight(HttpServletRequest req) {
    return req.getMethod().equals("OPTIONS")
        && !isBlank(req.getHeader("Access-Control-Request-Method"));
  }

  private boolean isTextPlain(HttpServletRequest req) {
    final String contentType = req.getContentType();
    return contentType != null && "text/plain".equals(contentType.split(";")[0]);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    final HttpServletRequest req = (HttpServletRequest) request;
    final HttpServletResponse res = (HttpServletResponse) response;
    final String origin = req.getHeader("Origin");
    final String host = req.getHeader("Host");

    if (corsOriginPattern == null || !isCrossOrigin(origin, host)) {
      chain.doFilter(request, response);
      return;
    }
    if (!isOriginAllowed(origin)) {
      res.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    res.addHeader("Access-Control-Allow-Origin", origin);
    res.addHeader("Access-Control-Allow-Credentials", corsAllowCredentials);

    // Handle preflight request
    if (isPreflight(req)) {
      res.addHeader("Access-Control-Allow-Methods", corsAllowMethods);
      res.addHeader("Access-Control-Allow-Headers", corsAllowHeaders);
      res.addHeader("Access-Control-Max-Age", corsMaxAge);
      res.setStatus(HttpServletResponse.SC_OK);
      return;
    }

    if (!isBlank(corsExposeHeaders)) {
      res.addHeader("Access-Control-Expose-Headers", corsExposeHeaders);
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
      return "content-type".equals(name.toLowerCase()) ? CONTENT_TYPE_JSON : super.getHeader(name);
    }
  }
}
