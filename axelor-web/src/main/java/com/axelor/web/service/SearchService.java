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
package com.axelor.web.service;

import com.axelor.mail.service.MailService;
import com.axelor.meta.service.MetaService;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.google.inject.servlet.RequestScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/search")
public class SearchService {

  @Inject private MetaService metaService;

  @Inject private MailService mailService;

  @POST
  public Response run(Request request) {
    return metaService.runSearch(request);
  }

  @POST
  @Path("emails")
  @SuppressWarnings("all")
  public Response emails(Request request) {
    final Response response = new Response();
    final String matching = (String) request.getData().get("search");
    final List selected = (List) request.getData().get("selected");
    final List<InternetAddress> addresses =
        mailService.findEmails(matching, selected, request.getLimit());
    final List<Object> data = new ArrayList<>();

    for (InternetAddress address : addresses) {
      final Map<String, String> item = new HashMap<>();
      item.put("address", address.getAddress());
      item.put("personal", address.getPersonal());
      data.add(item);
    }

    response.setData(data);
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  @GET
  @Path("menu")
  public Response menu(
      @QueryParam("parent") @DefaultValue("") String parent,
      @QueryParam("category") @DefaultValue("") String category) {
    Response response = new Response();
    try {
      response.setData(metaService.getActionMenus(parent, category));
      response.setStatus(Response.STATUS_SUCCESS);
    } catch (Exception e) {
      e.printStackTrace();
      response.setException(e);
    }
    return response;
  }
}
