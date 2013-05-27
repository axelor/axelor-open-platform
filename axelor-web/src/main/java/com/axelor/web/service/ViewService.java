package com.axelor.web.service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Permission;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
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
				if (p.getTarget() != null) {
					map.put("perms", perms(p.getTarget()));
				}
				fields.add(map);
			}
		}
		
		Map<String, Object> data = Maps.newHashMap();
		data.put("perms", perms(modelClass));
		data.put("fields", fields);

		response.setData(data);
		
		return response;
	}
	
	private Map<String, Object> perms(Class<?> model) {
		User user = AuthUtils.getUser();
		if (user == null || user.getGroup() == null
				|| "admin".equals(user.getCode())
				|| "admins".equals(user.getGroup().getCode())) {
			return null;
		}
		
		TypedQuery<Permission> q = JPA.em().createQuery(
				"SELECT p FROM User u " +
				"LEFT JOIN u.group AS g " +
				"LEFT JOIN g.permissions AS p " +
				"WHERE u.code = :code AND p.object = :object", Permission.class);

		q.setParameter("code", user.getCode());
		q.setParameter("object", model.getName());
		q.setMaxResults(1);

		Permission p;
		try {
			p = q.getResultList().get(0);
		} catch (IndexOutOfBoundsException e){
			return null;
		}
		
		Map<String, Object> map = Maps.newHashMap();
		map.put("read", p.getCanRead());
		map.put("write", p.getCanWrite());
		map.put("create", p.getCanCreate());
		map.put("remove", p.getCanRemove());

		return map;
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
		final List<Object> all = Lists.newArrayList();
		final MetaSelect select = MetaSelect.findByName(property.getSelection());
		if (select == null || select.getItems() == null) {
			return all;
		}
		for(MetaSelectItem item : select.getItems()) {
			all.add(ImmutableMap.of("value", item.getValue(), "title", JPA.translate(item.getTitle())));
		}
		return all;
	}
	
	@GET
	@Path("chart/{name}")
	public Response get(@PathParam("name") String name) {
		final MultivaluedMap<String, String> params = getUriInfo().getQueryParameters(true);
		final Map<String, Object> context = Maps.newHashMap();
		final Request request = new Request();

		for(String key : params.keySet()) {
			List<String> values = params.get(key);
			if (values.size() == 1) {
				context.put(key, values.get(0));
			} else {
				context.put(key, values);
			}
		}
		request.setData(context);

		return service.getChart(name, request);
	}

	@POST
	@Path("chart/{name}")
	public Response get(@PathParam("name") String name, Request request) {
		return service.getChart(name, request);
	}
}
