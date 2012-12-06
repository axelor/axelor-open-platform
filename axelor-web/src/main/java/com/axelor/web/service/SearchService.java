package com.axelor.web.service;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.axelor.meta.service.MetaService;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/search")
public class SearchService {
	
	@Inject
	private MetaService service;
	
	@POST
	public Response run(Request request) {
		return service.runSearch(request);
	}
	
	@GET
	@Path("menu")
	public Response menu(@QueryParam("parent") @DefaultValue("") String parent,
			@QueryParam("category") @DefaultValue("") String category) {
		Response response = new Response();
		try {
			response.setData(service.getActionMenus(parent, category));
			response.setStatus(Response.STATUS_SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			response.setException(e);
		}
		return response;
	}
}