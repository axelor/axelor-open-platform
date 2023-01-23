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

import com.axelor.auth.AuthUtils;
import com.axelor.meta.ActionExecutor;
import com.axelor.meta.service.menu.MenuService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.Response;
import com.axelor.ui.QuickMenuService;
import com.google.inject.servlet.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/action")
public class ActionService extends AbstractService {

  @Inject private MenuService menuService;

  @Inject private ActionExecutor actionExecutor;

  @Inject private QuickMenuService quickMenus;

  @GET
  @Path("menu/all")
  public Response all() {
    Response response = new Response();
    try {
      response.setData(menuService.getMenus(AuthUtils.getUser()));
      response.setStatus(Response.STATUS_SUCCESS);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) LOG.error(e.getMessage(), e);
      response.setException(e);
    }
    return response;
  }

  @GET
  @Path("menu/quick")
  public Response quickMenuBar() {
    Response response = new Response();
    try {
      response.setData(quickMenus.get());
      response.setStatus(Response.STATUS_SUCCESS);
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      response.setException(e);
    }
    return response;
  }

  @POST
  public Response execute(ActionRequest request) {
    return actionExecutor.execute(request);
  }

  @POST
  @Path("{action}")
  public Response execute(@PathParam("action") String action, ActionRequest request) {
    request.setAction(action);
    return actionExecutor.execute(request);
  }
}
