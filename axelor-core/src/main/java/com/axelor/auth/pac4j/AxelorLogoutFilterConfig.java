/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.inject.Beans;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;

public class AxelorLogoutFilterConfig implements FilterConfig {

  @Override
  public String getFilterName() {
    return AxelorLogoutFilter.class.getName();
  }

  @Override
  public ServletContext getServletContext() {
    return Beans.get(HttpServletRequest.class).getServletContext();
  }

  @Override
  public String getInitParameter(String name) {
    return null;
  }

  @Override
  public Enumeration<String> getInitParameterNames() {
    return null;
  }
}
