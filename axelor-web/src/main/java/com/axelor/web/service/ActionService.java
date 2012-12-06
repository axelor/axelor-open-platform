package com.axelor.web.service;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.axelor.meta.ActionHandler;
import com.axelor.meta.service.MetaService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.Response;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/action")
public class ActionService extends AbstractService {

	@Inject
	private MetaService service;
	
	@GET
	@Path("menu")
	public Response menu(@QueryParam("parent") @DefaultValue("") String parent) {
		Response response = new Response();
		try {
			response.setData(service.getMenus(parent));
			response.setStatus(Response.STATUS_SUCCESS);
		} catch (Exception e) {
			if (LOG.isErrorEnabled())
				LOG.error(e.getMessage(), e);
			response.setException(e);
		}
		return response;
	}
	
	@POST
	public Response execute(ActionRequest request) {
		ActionHandler handler = new ActionHandler(request, getInjector());
		return handler.execute();
	}
	
	@POST
	@Path("{action}")
	public Response execute(@PathParam("action") String action, ActionRequest request) {
		request.setAction(action);
		ActionHandler handler = new ActionHandler(request, getInjector());
		return handler.execute();
	}
}
