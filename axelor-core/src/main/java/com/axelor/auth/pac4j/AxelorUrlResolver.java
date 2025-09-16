/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.common.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
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
