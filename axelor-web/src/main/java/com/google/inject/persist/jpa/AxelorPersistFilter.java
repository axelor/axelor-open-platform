/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.google.inject.persist.jpa;

import com.google.inject.persist.PersistService;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

@Singleton
public class AxelorPersistFilter implements Filter {

  private final JpaPersistService unitOfWork;
  private final PersistService persistService;

  @Inject
  public AxelorPersistFilter(JpaPersistService unitOfWork, PersistService persistService) {
    this.unitOfWork = unitOfWork;
    this.persistService = persistService;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    persistService.start();
  }

  @Override
  public void destroy() {
    persistService.stop();
  }

  @Override
  public void doFilter(
      final ServletRequest servletRequest,
      final ServletResponse servletResponse,
      final FilterChain filterChain)
      throws IOException, ServletException {

    if (unitOfWork.isWorking()) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    unitOfWork.begin();
    try {
      filterChain.doFilter(servletRequest, servletResponse);
    } finally {
      unitOfWork.end();
    }
  }
}
