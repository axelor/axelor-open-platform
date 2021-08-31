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
package com.axelor.web.socket.inject;

import com.axelor.inject.Beans;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.servlet.http.HttpSession;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;

public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {

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
    HttpSession httpSession = (HttpSession) request.getHttpSession();
    sec.getUserProperties().put(HttpSession.class.getName(), httpSession);
    sec.getUserProperties().put(Subject.class.getName(), SecurityUtils.getSubject());
    sec.getUserProperties()
        .put(SecurityManager.class.getName(), SecurityUtils.getSecurityManager());
  }
}
