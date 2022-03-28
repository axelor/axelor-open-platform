/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import com.axelor.auth.AuthUtils;
import com.axelor.mail.web.MailController;
import com.axelor.meta.ActionExecutor;
import com.axelor.meta.service.menu.MenuService;
import com.axelor.meta.service.tags.TagsService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.axelor.team.web.TaskController;
import com.google.inject.servlet.RequestScoped;
import java.util.Collections;
import java.util.List;
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

  @Inject private TagsService tagsService;

  @Inject private MailController mailController;

  @Inject private TaskController teamController;

  @Inject private ActionExecutor actionExecutor;

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
  @Path("menu/tags")
  public Response tags() {
    return tags(false, Collections.emptyList());
  }

  @POST
  @Path("menu/tags")
  public Response tags(TagRequest request) {
    return tags(true, request.getNames());
  }

  private Response tags(boolean inNamesOnly, List<String> names) {
    final ActionResponse response = new ActionResponse();
    try {
      response.setValue("tags", tagsService.get(names));
      response.setStatus(Response.STATUS_SUCCESS);
      mailController.countMail(null, response);
      teamController.countTasks(null, response);
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

  private static class TagRequest {
    private List<String> names;

    public List<String> getNames() {
      return names;
    }
  }
}
