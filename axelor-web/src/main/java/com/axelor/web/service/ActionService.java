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

import com.axelor.mail.web.MailController;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.schema.views.MenuItem;
import com.axelor.meta.service.MetaService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.axelor.team.web.TaskController;
import com.google.inject.servlet.RequestScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  @Inject private MetaService service;

  @Inject private TaskController teamController;

  @Inject private MailController mailController;

  @GET
  @Path("menu/all")
  public Response all() {
    Response response = new Response();
    try {
      response.setData(service.getMenus(false));
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
    final ActionResponse response = new ActionResponse();
    final List<Object> tags = new ArrayList<>();
    try {
      for (MenuItem item : service.getMenus(true)) {
        Map<String, Object> tag = new HashMap<>();
        tag.put("name", item.getName());
        tag.put("tag", item.getTag());
        tag.put("tagStyle", item.getTagStyle());
        tags.add(tag);
      }
      response.setValue("tags", tags);
      response.setStatus(Response.STATUS_SUCCESS);
      mailController.countMail(null, response);
      teamController.countTasks(null, response);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) LOG.error(e.getMessage(), e);
      response.setException(e);
    }
    return response;
  }

  @POST
  public Response execute(ActionRequest request) {
    return new ActionHandler(request).execute();
  }

  @POST
  @Path("{action}")
  public Response execute(@PathParam("action") String action, ActionRequest request) {
    request.setAction(action);
    return new ActionHandler(request).execute();
  }
}
