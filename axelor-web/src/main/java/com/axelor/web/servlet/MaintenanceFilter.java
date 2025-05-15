/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
