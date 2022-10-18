/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import com.axelor.common.StringUtils;
import java.io.IOException;
import java.util.function.Supplier;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Wrap the request to reflect the original protocol, scheme, Host and prefix using the
 * "X-Forwarded-*" headers.
 */
@Singleton
public class ProxyFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    chain.doFilter(new ProxyHttpServletRequestWrapper((HttpServletRequest) request), response);
  }

  @Override
  public void destroy() {}

  /** Wrap the request and use `X-Forwarded-*` headers */
  private static class ProxyHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final Supplier<HttpServletRequest> delegate;

    private final String forwardedProto;
    private final int forwardedPort;
    private final String forwardedHost;
    private final String forwardedPrefix;
    private final String forwardedFor;
    private final String baseUrl;
    private String requestUri;
    private String requestUrl;
    private String actualRequestUri;

    public ProxyHttpServletRequestWrapper(HttpServletRequest request) {
      super(request);
      this.delegate = () -> (HttpServletRequest) getRequest();
      this.actualRequestUri = this.delegate.get().getRequestURI();

      this.forwardedProto = initForwardedProto(request);
      this.forwardedPort = initForwardedPort(request);
      this.forwardedHost = initForwardedHost(request);
      this.forwardedPrefix = initForwardedPrefix(request);

      this.baseUrl =
          this.getScheme()
              + "://"
              + this.getServerName()
              + ((getServerPort() == 443 || getServerPort() == 80) ? "" : ":" + getServerPort());

      this.forwardedFor = initForwardedFor(request);
      this.requestUri = initRequestUri();
      this.requestUrl = initRequestUrl();
    }

    @Override
    public String getScheme() {
      return this.forwardedProto != null ? this.forwardedProto : this.delegate.get().getScheme();
    }

    @Override
    public String getServerName() {
      return this.forwardedHost != null ? this.forwardedHost : this.delegate.get().getServerName();
    }

    @Override
    public int getServerPort() {
      return this.forwardedPort == -1 ? this.delegate.get().getServerPort() : this.forwardedPort;
    }

    @Override
    public boolean isSecure() {
      return "https".equals(getScheme()) || "wss".equals(getScheme());
    }

    @Override
    public String getContextPath() {
      return this.forwardedPrefix != null
          ? this.forwardedPrefix
          : this.delegate.get().getContextPath();
    }

    @Override
    public String getRemoteHost() {
      return this.forwardedFor != null ? this.forwardedFor : this.delegate.get().getRemoteHost();
    }

    @Override
    public String getRemoteAddr() {
      return this.forwardedFor != null ? this.forwardedFor : this.delegate.get().getRemoteAddr();
    }

    @Override
    public String getRequestURI() {
      if (this.requestUri == null) {
        return this.delegate.get().getRequestURI();
      }
      computeUri();
      return this.requestUri;
    }

    @Override
    public StringBuffer getRequestURL() {
      computeUri();
      return new StringBuffer(this.requestUrl);
    }

    private String initForwardedHost(HttpServletRequest request) {
      String hostHeader = request.getHeader("X-Forwarded-Host");
      if (StringUtils.notBlank(hostHeader)) {
        return StringUtils.splitToArray(hostHeader, ",")[0];
      }
      return null;
    }

    private int initForwardedPort(HttpServletRequest request) {
      String portHeader = request.getHeader("X-Forwarded-Port");
      if (StringUtils.notBlank(portHeader)) {
        return Integer.parseInt(StringUtils.splitToArray(portHeader, ",")[0]);
      }
      return -1;
    }

    private String initForwardedProto(HttpServletRequest request) {
      String protoHeader = request.getHeader("X-Forwarded-Proto");
      if (StringUtils.notBlank(protoHeader)) {
        return StringUtils.splitToArray(protoHeader, ",")[0];
      }
      return null;
    }

    private String initForwardedPrefix(HttpServletRequest request) {
      String prefixHeader = request.getHeader("X-Forwarded-Prefix");
      if (StringUtils.notBlank(prefixHeader)) {
        StringBuilder prefix = new StringBuilder(prefixHeader.length());
        String[] parts = StringUtils.splitToArray(prefixHeader, ",");
        for (String item : parts) {
          int endIndex = item.length();
          while (endIndex > 0 && item.charAt(endIndex - 1) == '/') {
            endIndex--;
          }
          prefix.append(endIndex != item.length() ? item.substring(0, endIndex) : item);
        }
        return prefix.toString();
      }
      return null;
    }

    private String initForwardedFor(HttpServletRequest request) {
      String protoHeader = request.getHeader("X-Forwarded-For");
      if (StringUtils.notBlank(protoHeader)) {
        return StringUtils.splitToArray(protoHeader, ",")[0];
      }
      return null;
    }

    private String initRequestUri() {
      if (this.forwardedPrefix != null) {
        return this.forwardedPrefix
            + this.delegate
                .get()
                .getRequestURI()
                .replaceFirst(this.delegate.get().getContextPath(), "");
      }
      return null;
    }

    private String initRequestUrl() {
      return this.baseUrl
          + (this.requestUri != null ? this.requestUri : this.delegate.get().getRequestURI());
    }

    private void computeUri() {
      if (!this.actualRequestUri.equals(this.delegate.get().getRequestURI())) {
        this.actualRequestUri = this.delegate.get().getRequestURI();
        this.requestUri = initRequestUri();
        this.requestUrl = initRequestUrl();
      }
    }
  }
}
