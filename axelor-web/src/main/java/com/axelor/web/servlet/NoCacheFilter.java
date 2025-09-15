/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.servlet;

import com.axelor.app.AppSettings;
import com.axelor.app.internal.AppFilter;
import com.axelor.common.StringUtils;
import jakarta.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Singleton
public class NoCacheFilter implements Filter {

  public static final List<String> STATIC_URL_PATTERNS =
      List.of(
          "/static/*",
          "/public/*",
          "/images/*",
          "/javascript/*",
          "/dist/*",
          "/lib/*",
          "/img/*",
          "/ico/*",
          "/css/*",
          "/js/*",
          "/*.js",
          "/*.css",
          "/*.png",
          "/*.jpg");

  private static final String CACHE_BUSTER_PARAM = "" + Calendar.getInstance().getTimeInMillis();

  private boolean production;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.production = AppSettings.get().isProduction();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    final HttpServletRequest req = (HttpServletRequest) request;
    final HttpServletResponse res = (HttpServletResponse) response;

    final String uri = req.getRequestURI();
    final boolean busted = req.getParameterMap().containsKey(CACHE_BUSTER_PARAM);

    // TODO: Move messages.js to another appropriate endpoint
    if ("/js/messages.js".equals(uri)) {
      // Exclude messages.js from caching
      chain.doFilter(request, response);
      return;
    }

    // if gzip bundle, set content-encoding header
    if (uri.contains(".gzip.")) {
      res.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
    }

    if (production) {
      if (busted) {
        res.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable");
      } else {
        final StringBuilder requestUri = new StringBuilder(uri + "?" + CACHE_BUSTER_PARAM);
        final String queryString = req.getQueryString();

        if (StringUtils.notBlank(queryString)) {
          requestUri.append("&").append(queryString);
        }

        res.sendRedirect(
            URI.create(AppFilter.getBaseURL()).resolve(requestUri.toString()).toString());
        return;
      }
    } else {
      res.setHeader("Expires", "Fri, 01 Jan 1990 00:00:00 GMT");
      res.setHeader("Last-Modified", new Date().toString());
      if (uri.matches(".*\\.(eot|ttf|woff|woff2).*")) {
        res.setHeader(
            HttpHeaders.CACHE_CONTROL,
            "no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
      } else {
        res.setHeader(
            HttpHeaders.CACHE_CONTROL,
            "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
        res.setHeader("Pragma", "no-cache");
      }
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {}
}
