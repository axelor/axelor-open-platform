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
package com.axelor.auth.cas;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.web.filter.authc.UserFilter;
import org.apache.shiro.web.util.WebUtils;

public class AuthCasUserFilter extends UserFilter {

  @Inject
  @Override
  public void setLoginUrl(@Named("shiro.cas.login.url") String loginUrl) {
    super.setLoginUrl(loginUrl);
  }

  @Override
  protected boolean isAccessAllowed(
      ServletRequest request, ServletResponse response, Object mappedValue) {
    return super.isAccessAllowed(request, response, mappedValue);
  }

  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response)
      throws Exception {
    if (isXHR(request)) {
      if (isLogin(request)) {
        return doLogin(request, response);
      }
      HttpServletResponse res = (HttpServletResponse) response;
      res.setStatus(302);
      return false;
    }
    return super.onAccessDenied(request, response);
  }

  private boolean doLogin(ServletRequest request, ServletResponse response) throws Exception {
    return false;
  }

  private boolean isXHR(ServletRequest request) {
    return "XMLHttpRequest".equals(((HttpServletRequest) request).getHeader("X-Requested-With"));
  }

  private boolean isLogin(ServletRequest request) {
    return pathsMatch("/login.jsp", request)
        && WebUtils.toHttp(request).getMethod().toUpperCase().equals(POST_METHOD);
  }
}
