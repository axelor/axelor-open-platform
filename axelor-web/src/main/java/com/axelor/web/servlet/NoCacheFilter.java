/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.web.servlet;

import com.axelor.app.AppSettings;
import com.axelor.app.internal.AppFilter;
import com.axelor.common.StringUtils;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class NoCacheFilter implements Filter {

  public static final String[] STATIC_URL_PATTERNS = {
    "/static/*",
    "/public/*",
    "/partials/*",
    "/images/*",
    "/javascript/*",
    "/dist/*",
    "/lib/*",
    "/img/*",
    "/ico/*",
    "/css/*",
    "/js/*",
    "*.js",
    "*.css",
    "*.png",
    "*.jpg"
  };

  private static final List<String> EXCLUDED_EXTS = ImmutableList.of(".jsp");

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

    // if gzip bundle, set content-encoding header
    if (uri.contains(".gzip.")) {
      res.setHeader("Content-Encoding", "gzip");
    }

    if (production && !busted && EXCLUDED_EXTS.stream().noneMatch(uri::endsWith)) {
      final StringBuilder requestUri = new StringBuilder(uri + "?" + CACHE_BUSTER_PARAM);
      final String queryString = req.getQueryString();

      if (StringUtils.notBlank(queryString)) {
        requestUri.append("&").append(queryString);
      }

      res.sendRedirect(
          URI.create(AppFilter.getBaseURL()).resolve(requestUri.toString()).toString());
      return;
    }

    if (!production) {
      res.setHeader("Expires", "Fri, 01 Jan 1990 00:00:00 GMT");
      res.setHeader("Last-Modified", new Date().toString());
      if (uri.matches(".*\\.(eot|ttf|woff|woff2).*")) {
        res.setHeader(
            "Cache-Control", "no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
      } else {
        res.setHeader(
            "Cache-Control",
            "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
        res.setHeader("Pragma", "no-cache");
      }
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {}
}
