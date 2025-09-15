/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.servlet;

import com.axelor.app.AppSettings;
import jakarta.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;

/** Caches version/hash-named assets for production */
@Singleton
public class CacheAssetsFilter implements Filter {

  public static final String URL_PATTERN = "/assets/*";

  private boolean production;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.production = AppSettings.get().isProduction();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (production) {
      var httpResponse = (HttpServletResponse) response;
      httpResponse.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable");
    }

    chain.doFilter(request, response);
  }
}
