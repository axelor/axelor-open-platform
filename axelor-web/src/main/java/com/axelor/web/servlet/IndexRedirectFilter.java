/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.servlet;

import com.axelor.common.StringUtils;
import jakarta.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Redirects direct requests for {@code /index.html} to the context root ({@code /}).
 *
 * <p>This filter normalizes the URL so the application always runs from the context root.
 */
@Singleton
public class IndexRedirectFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    var req = (HttpServletRequest) request;
    var res = (HttpServletResponse) response;

    var location = new StringBuilder(req.getContextPath()).append('/');
    var queryString = req.getQueryString();

    if (StringUtils.notBlank(queryString)) {
      location.append('?').append(queryString);
    }

    res.sendRedirect(location.toString());
  }
}
