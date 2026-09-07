/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.socket.inject;

import com.axelor.app.AppSettings;
import com.axelor.common.StringUtils;
import com.axelor.db.tenants.TenantModule;
import com.axelor.db.tenants.TenantResolver;
import com.axelor.inject.Beans;
import jakarta.annotation.Nullable;
import jakarta.websocket.Extension;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {

  static final String TENANT_ID = "tenant-id";
  static final String TENANT_HOST = "tenant-host";

  private static final Logger log = LoggerFactory.getLogger(WebSocketConfigurator.class);

  @Override
  public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
    return Beans.get(endpointClass);
  }

  @Override
  public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
    return requested.stream().filter(supported::contains).findFirst().orElse("");
  }

  @Override
  public List<Extension> getNegotiatedExtensions(
      List<Extension> installed, List<Extension> requested) {
    return requested.stream()
        .filter(e -> installed.stream().anyMatch(x -> Objects.equals(x.getName(), e.getName())))
        .collect(Collectors.toList());
  }

  /**
   * Checks the <code>Origin</code> header of the handshake request.
   *
   * <p>Only same-origin is allowed.
   *
   * @param originHeaderValue the <code>Origin</code> header value
   * @return true if the handshake is allowed
   */
  @Override
  public boolean checkOrigin(String originHeaderValue) {
    // Browsers always send the Origin header on the handshake (RFC 6455 §4.1). Native clients
    // don't, and are not subject to CSRF.
    if (StringUtils.isBlank(originHeaderValue)) {
      return true;
    }

    if (isSameOrigin(originHeaderValue, AppSettings.get().getBaseURL())
        || (TenantModule.isEnabled()
            && isTenantOrigin(originHeaderValue, TenantResolver.currentTenantHost()))) {
      return true;
    }

    log.debug(
        "Rejecting websocket handshake, Origin header value {} not allowed", originHeaderValue);

    return false;
  }

  /**
   * Whether the given origin is same origin as the given base url.
   *
   * @param origin the <code>Origin</code> header value
   * @param baseUrl the base url of the application
   * @return true if the origin is same origin as the base url
   */
  static boolean isSameOrigin(String origin, @Nullable String baseUrl) {
    final String expected = toOrigin(baseUrl);

    return !expected.isEmpty() && expected.equals(toOrigin(origin));
  }

  /**
   * Whether the given origin is the host of the current tenant.
   *
   * <p>For a multi-tenant application, the base url is built from the tenant host with a scheme
   * inferred from the application mode instead of the actual request, so the host is compared with
   * both schemes.
   *
   * @param origin the <code>Origin</code> header value
   * @param tenantHost the host of the current tenant, may be <code>null</code>
   * @return true if the origin is the host of the current tenant
   */
  static boolean isTenantOrigin(String origin, @Nullable String tenantHost) {
    if (StringUtils.isBlank(tenantHost)) {
      return false;
    }

    final String actual = toOrigin(origin);

    return !actual.isEmpty()
        && (actual.equals(toOrigin("http://" + tenantHost))
            || actual.equals(toOrigin("https://" + tenantHost)));
  }

  /**
   * Reduces the given url to its origin, ie. <code>scheme://host[:port]</code>, leaving out the
   * default port of the scheme, so that it can be compared with an <code>Origin</code> header
   * value.
   *
   * @param url the url to reduce
   * @return the origin, or an empty string if the url cannot be parsed
   */
  private static String toOrigin(String url) {
    if (StringUtils.isBlank(url)) {
      return "";
    }

    final URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      return "";
    }

    final String scheme = uri.getScheme();
    final String host = uri.getHost();

    if (scheme == null || host == null) {
      return "";
    }

    final String origin = scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT);
    final int port = uri.getPort();

    return port == -1 || port == defaultPort(scheme) ? origin : origin + ":" + port;
  }

  private static int defaultPort(String scheme) {
    return switch (scheme.toLowerCase(Locale.ROOT)) {
      case "https", "wss" -> 443;
      default -> 80;
    };
  }

  @Override
  public void modifyHandshake(
      ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
    final Map<String, Object> properties = sec.getUserProperties();
    properties.put(Subject.class.getName(), SecurityUtils.getSubject());
    properties.put(SecurityManager.class.getName(), SecurityUtils.getSecurityManager());

    final String tenantId = TenantResolver.currentTenantIdentifier();
    if (tenantId != null) {
      properties.put(TENANT_ID, tenantId);
    }
    final String tenantHost = TenantResolver.currentTenantHost();
    if (tenantHost != null) {
      properties.put(TENANT_HOST, tenantHost);
    }
  }
}
