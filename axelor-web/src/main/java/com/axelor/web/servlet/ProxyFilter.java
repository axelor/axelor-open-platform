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

import com.axelor.common.StringUtils;
import java.io.IOException;
import java.util.function.Supplier;
import jakarta.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrap the request to reflect the original protocol, scheme, Host and prefix using the
 * "X-Forwarded-*" headers.
 */
@Singleton
public class ProxyFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(ProxyFilter.class);

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
    private String forwardedHost;
    private int forwardedPort = -1;
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
      initForwardedHostPort(request);
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

    private void initForwardedHostPort(HttpServletRequest request) {
      String hostHeader = request.getHeader("X-Forwarded-Host");
      if (StringUtils.notBlank(hostHeader)) {
        String host = StringUtils.splitToArray(hostHeader, ",")[0];
        int portSeparatorIdx = host.lastIndexOf(':');
        int squareBracketIdx = host.lastIndexOf(']');
        if (portSeparatorIdx > squareBracketIdx) {
          if (squareBracketIdx == -1 && host.indexOf(':') != portSeparatorIdx) {
            log.error("Invalid IPv4 address: {}", host);
          } else {
            this.forwardedHost = host.substring(0, portSeparatorIdx);
            this.forwardedPort = Integer.parseInt(host, portSeparatorIdx + 1, host.length(), 10);
          }
        } else {
          this.forwardedHost = host;
        }
      }

      String portHeader = request.getHeader("X-Forwarded-Port");
      if (StringUtils.notBlank(portHeader)) {
        this.forwardedPort = Integer.parseInt(StringUtils.splitToArray(portHeader, ",")[0]);
      }
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
        String initialRequestURI = this.delegate.get().getRequestURI();
        if (initialRequestURI.startsWith(this.delegate.get().getContextPath())) {
          initialRequestURI =
              initialRequestURI.substring(this.delegate.get().getContextPath().length());
        }
        return this.forwardedPrefix + initialRequestURI;
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
