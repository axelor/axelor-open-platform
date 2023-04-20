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
package com.axelor.web.service;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.common.ObjectUtils;
import com.axelor.web.internal.AppInfo;
import com.google.inject.servlet.RequestScoped;
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

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/public/app")
public class PublicInfoService extends AbstractService {

  @Context private HttpServletRequest request;

  @Inject private AuthPac4jInfo pac4jInfo;

  private static final AppSettings SETTINGS = AppSettings.get();

  @GET
  @Path("info")
  public Map<String, Object> info() {
    return info(request.getServletContext());
  }

  private Map<String, Object> info(final ServletContext context) {
    final Map<String, Object> map = new HashMap<>();

    map.put("application", appInfo());
    if (ObjectUtils.notEmpty(pac4jInfo.getCentralClients())) {
      map.put("clients", clientsInfo());
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
      clientMap.put("icon", info.get("icon"));
      if (ObjectUtils.notEmpty(info.get("title"))) {
        clientMap.put("title", info.get("title"));
      }

      clients.add(clientMap);
    }

    return clients;
  }

  private Map<String, Object> appInfo() {
    final Map<String, Object> map = new HashMap<>();
    final AppInfo info = new AppInfo();

    map.put("name", SETTINGS.get(AvailableAppSettings.APPLICATION_NAME));
    map.put(
        "copyright",
        SETTINGS.get(
            AvailableAppSettings.APPLICATION_COPYRIGHT,
            "&copy; 2005–{year} Axelor. All Rights Reserved."));
    map.put("logo", info.getLogo());
    map.put("language", info.getPageLang());
    map.put("callbackUrl", pac4jInfo.getCallbackUrl());

    return map;
  }
}
