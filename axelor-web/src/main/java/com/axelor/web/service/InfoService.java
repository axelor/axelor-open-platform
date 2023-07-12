/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.web.service;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.auth.pac4j.ClientListProvider;
import com.axelor.common.MimeTypesUtils;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.web.internal.AppInfo;
import com.google.inject.servlet.RequestScoped;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/public/app")
public class InfoService extends AbstractService {

  @Context private HttpServletRequest request;

  private final AppInfo info;
  private final AuthPac4jInfo pac4jInfo;

  private final String defaultClient;
  private final boolean exclusive;

  private static final AppSettings SETTINGS = AppSettings.get();

  @Inject
  public InfoService(
      AppInfo appInfo, AuthPac4jInfo pac4jInfo, ClientListProvider clientListProvider) {
    this.info = appInfo;
    this.pac4jInfo = pac4jInfo;
    this.defaultClient = clientListProvider.getDefaultClientName();
    this.exclusive = clientListProvider.isExclusive();
  }

  /**
   * Retrieves either application login information or session information if the user is logged in.
   */
  @GET
  @Path("info")
  public Map<String, Object> info() {
    final User user = AuthUtils.getUser();
    return user == null ? loginInfo() : info.info(user);
  }

  private Map<String, Object> loginInfo() {
    return Map.of("application", appInfo(), "authentication", authInfo());
  }

  private Map<String, Object> appInfo() {
    final Map<String, Object> map = new HashMap<>();

    map.put("name", SETTINGS.get(AvailableAppSettings.APPLICATION_NAME));
    map.put("description", SETTINGS.get(AvailableAppSettings.APPLICATION_DESCRIPTION));
    map.put("copyright", SETTINGS.get(AvailableAppSettings.APPLICATION_COPYRIGHT));
    map.put("theme", info.getTheme());
    map.put("logo", info.getLogo());
    map.put("icon", info.getIcon());
    map.put("lang", info.getPageLang());

    return map;
  }

  private Map<String, Object> authInfo() {
    final Map<String, Object> map = new HashMap<>();
    map.put("callbackUrl", pac4jInfo.getCallbackUrl());

    if (ObjectUtils.notEmpty(pac4jInfo.getCentralClients())) {
      map.put("clients", clientsInfo());
    }

    if (StringUtils.notEmpty(defaultClient)) {
      map.put("defaultClient", defaultClient);
    }

    if (exclusive) {
      map.put("exclusive", exclusive);
    }

    return map;
  }

  private List<Object> clientsInfo() {
    final List<Object> clients = new ArrayList<>();

    for (String client : pac4jInfo.getCentralClients()) {
      Map<String, Object> clientMap = new HashMap<>();
      Map<String, String> info = pac4jInfo.getClientInfo(client);
      if (info == null) {
        continue;
      }

      clientMap.put("name", client);

      final String icon = info.get("icon");
      if (StringUtils.notEmpty(icon)) {
        clientMap.put("icon", icon);
      }

      final String title = info.get("title");
      if (StringUtils.notEmpty(title)) {
        clientMap.put("title", title);
      }

      clients.add(clientMap);
    }

    return clients;
  }

  @GET
  @Path("logo")
  public Response getLogoContent() {
    return getImageContent(info.getLogo());
  }

  @GET
  @Path("icon")
  public Response getIconContent() {
    return getImageContent(info.getIcon());
  }

  private Response getImageContent(String pathString) {
    if (StringUtils.notEmpty(pathString)) {
      final ServletContext context = request.getServletContext();
      try (final InputStream inputStream = context.getResourceAsStream(pathString)) {
        if (inputStream != null) {
          final byte[] imageData = inputStream.readAllBytes();
          final String mediaType = MimeTypesUtils.getContentType(pathString);
          return Response.ok(imageData).type(mediaType).build();
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    return Response.status(Response.Status.NOT_FOUND).build();
  }
}
