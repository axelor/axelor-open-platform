/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j;

import com.axelor.common.StringUtils;
import javax.servlet.http.HttpServletRequest;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.WebContextHelper;
import org.pac4j.core.http.url.DefaultUrlResolver;
import org.pac4j.jee.context.JEEContext;

public class AxelorUrlResolver extends DefaultUrlResolver {

  public AxelorUrlResolver() {}

  @Override
  public String compute(String url, WebContext context) {
    final var relativeUrl =
        url != null
            && !url.startsWith(HttpConstants.SCHEME_HTTP)
            && !url.startsWith(HttpConstants.SCHEME_HTTPS);

    if (context != null && relativeUrl) {
      HttpServletRequest request = ((JEEContext) context).getNativeRequest();
      final StringBuilder sb = new StringBuilder();

      sb.append(request.getScheme()).append("://").append(request.getServerName());

      final boolean notDefaultHttpPort =
          WebContextHelper.isHttp(context)
              && context.getServerPort() != HttpConstants.DEFAULT_HTTP_PORT;
      final boolean notDefaultHttpsPort =
          WebContextHelper.isHttps(context)
              && context.getServerPort() != HttpConstants.DEFAULT_HTTPS_PORT;
      if (notDefaultHttpPort || notDefaultHttpsPort) {
        sb.append(":").append(request.getServerPort());
      }

      if (StringUtils.notBlank(request.getContextPath())) {
        String contextPath =
            request.getContextPath().endsWith("/")
                ? request.getContextPath().substring(0, request.getContextPath().length() - 1)
                : request.getContextPath();
        sb.append(contextPath.startsWith("/") ? contextPath : "/" + contextPath);
      }
      sb.append(url.startsWith("/") ? url : "/" + url);

      return sb.toString();
    } else {
      return url;
    }
  }
}
