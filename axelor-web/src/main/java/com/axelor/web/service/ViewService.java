package com.axelor.web.service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
@Path("/meta")
public class ViewService extends AbstractService {

	@Inject
	private MetaService service;

	private Class<?> findClass(String name) {
		try {
			return Class.forName(name);
		} catch (Exception e) {
		}
		return null;
	}
	
	@GET
	@Path("models")
	public Response models() {
		return Resource.models(new Request());
	}

	@GET
	@Path("fields/{model}")
	public Response fields(@PathParam("model") String model) {

		final Response response = new Response();
		final Map<String, Object> meta = Maps.newHashMap();
		final Class<?> modelClass = findClass(model);
		final List<Object> fields = Lists.newArrayList();

		for (Property p : Mapper.of(modelClass).getProperties()) {
			fields.add(p.toMap());
		}

		meta.put("model", model);
		meta.put("fields", fields);

		response.setData(meta);
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}
	
	@GET
	@Path("views/{model}")
	public Response views(@PathParam("model") String model) {
		final MultivaluedMap<String, String> params = getUriInfo().getQueryParameters(true);
		final Map<String, String> views = Maps.newHashMap();
		for (String mode : params.keySet()) {
			views.put(mode, params.getFirst(mode));
		}
		return service.findViews(findClass(model), views);
	}
	
	@GET
	@Path("view")
	public Response view(
			@QueryParam("model") String model,
			@QueryParam("name") String name,
			@QueryParam("type") String type) {
		return service.findView(model, name, type);
	}

	@POST
	@Path("view/fields")
	public Response viewFields(Request request) {
		
		final String model = request.getModel();
		final List<String> names = request.getFields();

		final Class<?> modelClass = findClass(model);
		final Mapper mapper = Mapper.of(modelClass);

		final Response response = new Response();
		final List<Object> fields = Lists.newArrayList();

		for(String name : names) {
			Property p = findField(mapper, name);
			if (p != null) {
				Map<String, Object> map = p.toMap();
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
	
	private Property findField(final Mapper mapper, String name) {
		final Iterator<String> iter = Splitter.on(".").split(name).iterator();
		Mapper current = mapper;
		Property property = current.getProperty(iter.next());
		while(iter.hasNext()) {
			current = Mapper.of(property.getTarget());
			property = current.getProperty(iter.next());
		}
		return property;
	}

	private List<Object> findSelection(Property property) {
		if (property.getSelection() == null) {
			return null;
		}
		final List<MetaSelect> items = MetaSelect.all().filter("self.key = ?", property.getSelection()).fetch();
		final List<Object> all = Lists.newArrayList();
		for(MetaSelect ms : items) {
			all.add(ImmutableMap.of("value", ms.getValue(), "title", ms.getTitle()));
		}
		return all;
	}
}
