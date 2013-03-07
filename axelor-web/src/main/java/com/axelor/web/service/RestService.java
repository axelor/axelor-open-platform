package com.axelor.web.service;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.google.common.collect.ImmutableMap;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/rest/{model}")
public class RestService extends ResourceService {
	
	@GET
	public Response find(
			@QueryParam("limit")
			@DefaultValue("40") int limit,
			@QueryParam("offset")
			@DefaultValue("0") int offset,
			@QueryParam("q") String query) {
		
		Request request = new Request();
		request.setOffset(offset);
		request.setLimit(limit);
		return getResource().search(request);
	}
	
	@POST
	@Path("search")
	public Response find(Request request) {
		request.setModel(getModel());
		return getResource().search(request);
	}
	
	@POST
	public Response save(Request request) {
		request.setModel(getModel());
		return getResource().save(request);
	}
	
	@PUT
	public Response create(Request request) {
		request.setModel(getModel());
		return getResource().save(request);
	}
	
	@GET
	@Path("{id}")
	public Response read(
			@PathParam("id") long id) {
		return getResource().read(id);
	}
	
	@POST
	@Path("{id}/fetch")
	public Response fetch(
			@PathParam("id") long id, Request request) {
		return getResource().fetch(id, request);
	}
	
	@POST
	@Path("{id}")
	public Response update(@PathParam("id") long id, Request request) {
		request.setModel(getModel());
		return getResource().save(request);
	}
	
	@DELETE
	@Path("{id}")
	public Response delete(@PathParam("id") long id, @QueryParam("version") int version) {
		Request request = new Request();
		request.setModel(getModel());
		request.setData(ImmutableMap.of("id", (Object) id, "version", version));
		return getResource().remove(id, request);
	}
	
	@POST
	@Path("{id}/remove")
	public Response remove(@PathParam("id") long id, Request request) {
		request.setModel(getModel());
		return getResource().remove(id, request);
	}
	
	@POST
	@Path("removeAll")
	public Response remove(Request request) {
		request.setModel(getModel());
		return getResource().remove(request);
	}

	@GET
	@Path("{id}/copy")
	public Response copy(@PathParam("id") long id) {
		return getResource().copy(id);
	}
	
	@GET
	@Path("{id}/details")
	public Response details(@PathParam("id") long id) {
		Request request = new Request();
		Map<String, Object> data = new HashMap<String, Object>();
		
		data.put("id", id);
		request.setModel(getModel());
		request.setData(data);
		
		return getResource().getRecordName(request);
	}

	@POST
	@Path("verify")
	public Response verify(Request request) {
		request.setModel(getModel());
		return getResource().verify(request);
	}
}