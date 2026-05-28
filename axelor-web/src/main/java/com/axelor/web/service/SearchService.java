/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.service;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.mail.service.MailService;
import com.axelor.meta.service.MetaService;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.google.inject.servlet.RequestScoped;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.inject.Inject;
import jakarta.mail.internet.InternetAddress;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/search")
@Hidden
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
    int limit = request.getLimit();
    int maxPerPage =
        AppSettings.get().getInt(AvailableAppSettings.API_PAGINATION_MAX_PER_PAGE, 500);
    int defaultPerPage =
        AppSettings.get().getInt(AvailableAppSettings.API_PAGINATION_DEFAULT_PER_PAGE, 40);
    limit = limit > 0 ? Math.min(limit, maxPerPage) : defaultPerPage;
    final List<InternetAddress> addresses = mailService.findEmails(matching, selected, limit);
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
