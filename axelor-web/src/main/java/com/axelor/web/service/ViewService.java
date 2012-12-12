package com.axelor.web.service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.service.MetaService;
import com.axelor.rpc.Request;
import com.axelor.rpc.Resource;
import com.axelor.rpc.Response;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/meta/{model}")
public class ViewService extends ResourceService {

	@Inject
	private MetaService service;
	
	@RequestScoped
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/meta/models")
	public static class ModelService {
		
		@GET
		public Response models() {
			return Resource.models(new Request());
		}
	}
	
	@GET @SuppressWarnings("all")
	public Response get() {
		Response r1 = this.fields();
		Response r2 = this.views();
		
		Map<String, Object> data = Maps.newHashMap();
		data.putAll((Map) r1.getData());
		data.put("views", r2.getData());

		Response response = new Response();
		response.setData(data);
		return response;
	}
	
	@GET
	@Path("fields")
	public Response fields() {
		return getResource().fields();
	}
	
	@GET
	@Path("views")
	public Response views() {
		MultivaluedMap<String, String> params = getUriInfo().getQueryParameters(true);
		Map<String, String> views = Maps.newHashMap();
		for (String mode : params.keySet())
			views.put(mode, params.getFirst(mode));
		return service.findViews(entityClass(), views);
	}
	
	@GET
	@Path("view")
	public Response getView(@QueryParam("type") String type, @QueryParam("name") String name) {
		Map<String, String> view = Maps.newHashMap();
		view.put(type, name);
		return service.findViews(entityClass(), view);
	}
	
	private Property findField(String name) {
		Mapper mapper = Mapper.of(entityClass());
		Iterator<String> iter = Splitter.on(".").split(name).iterator();
		Property p = mapper.getProperty(iter.next());
		while(iter.hasNext()) {
			mapper = Mapper.of(p.getTarget());
			p = mapper.getProperty(iter.next());
		}
		return p;
	}
	
	private List<Object> findSelection(Property property) {
		if (property.getSelection() == null)
			return null;
		List<MetaSelect> items = MetaSelect.all().filter("self.key = ?", property.getSelection()).fetch();
		List<Object> all = Lists.newArrayList();
		for(MetaSelect ms : items) {
			all.add(ImmutableMap.of("value", ms.getValue(), "title", ms.getTitle()));
		}
		return all;
	}

	@GET
	@Path("view_fields")
	@SuppressWarnings("all")
	public Response getViewFields(@QueryParam("items") String items) {
		Response response = new Response();
		List<Object> fields = Lists.newArrayList();
		Iterable<String> names = Splitter.on(",").trimResults().split(items);
		
		for(String name : names) {
			Property p = findField(name);
			if (p != null) {
				Map map = (Map) p.toMap();
				map.put("name", name);
				if (p.getSelection() != null && !"".equals(p.getSelection().trim())) {
					map.put("selection", findSelection(p));
				}
				fields.add(map);
			}
		}

		response.setData(fields);
		return response;
	}
}
