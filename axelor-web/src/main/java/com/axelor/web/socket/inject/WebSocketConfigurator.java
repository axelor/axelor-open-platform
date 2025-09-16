/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.socket.inject;

import com.axelor.db.tenants.TenantResolver;
import com.axelor.inject.Beans;
import jakarta.websocket.Extension;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;

public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {

  static final String TENANT_ID = "tenant-id";
  static final String TENANT_HOST = "tenant-host";

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

  @Override
  public boolean checkOrigin(String originHeaderValue) {
    return true;
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
