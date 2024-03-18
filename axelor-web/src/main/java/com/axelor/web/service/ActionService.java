/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
import com.axelor.inject.Beans;
import com.axelor.meta.ActionExecutor;
import com.axelor.meta.db.repo.MetaMenuRepository;
import com.axelor.meta.service.menu.MenuService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.Resource;
import com.axelor.rpc.Response;
import com.axelor.ui.QuickMenuService;
import com.google.inject.servlet.RequestScoped;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.stream.Collectors;
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
@Tag(name = "Actions")
public class ActionService extends AbstractService {

  @Inject private MenuService menuService;

  @Inject private ActionExecutor actionExecutor;

  @Inject private QuickMenuService quickMenus;

  @GET
  @Path("menu/all")
  @Hidden
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
  @Hidden
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

  @GET
  @Path("menu/fav")
  @Hidden
  public Response favMenuBar() {
    Response response = new Response();
    MetaMenuRepository menus = Beans.get(MetaMenuRepository.class);
    try {
      response.setData(
          menus
              .all()
              .filter("self.user = :__user__ and self.link is not null")
              .order("-priority")
              .fetch()
              .stream()
              .map(x -> Resource.toMap(x, "id", "version", "name", "link", "priority"))
              .collect(Collectors.toList()));
      response.setStatus(Response.STATUS_SUCCESS);
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      response.setException(e);
    }
    return response;
  }

  @POST
  @Operation(
      summary = "Execute an action",
      description =
          "Execute an action along with the provided data context. "
              + "The action can be single or list of comma-separated actions. "
              + "An action can be either an xml action or a method call.")
  public Response execute(ActionRequest request) {
    return actionExecutor.execute(request);
  }

  @POST
  @Path("{action}")
  @Operation(
      summary = "Execute an action",
      description =
          "Execute an action along with the provided data context. "
              + "The action can be single or list of comma-separated actions. "
              + "An action can be either an xml action or a method call.")
  public Response execute(@PathParam("action") String action, ActionRequest request) {
    request.setAction(action);
    return actionExecutor.execute(request);
  }
}
