/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.servlet;

import com.axelor.auth.AuthUtils;
import com.axelor.web.service.MaintenanceService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;

@Singleton
public class MaintenanceFilter implements Filter {

  private MaintenanceService maintenanceService;

  @Inject
  public MaintenanceFilter(MaintenanceService maintenanceService) {
    this.maintenanceService = maintenanceService;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String requestURI = httpRequest.getRequestURI();
    String contextPath = httpRequest.getContextPath();
    String path = requestURI.substring(contextPath.length());

    if (!path.startsWith("/ws/public/app/")
        && maintenanceService.isMaintenanceMode(AuthUtils.getUser(), httpRequest)) {
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      httpResponse.setHeader(HttpHeaders.RETRY_AFTER, "30");
      httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      return;
    }

    chain.doFilter(request, response);
  }
}
