/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

import java.util.Collections;
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

import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.JpaSecurity;
import com.axelor.db.JpaSecurity.AccessType;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.MetaStore;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.AbstractWidget;
import com.axelor.meta.schema.views.Field;
import com.axelor.meta.schema.views.FormInclude;
import com.axelor.meta.schema.views.FormView;
import com.axelor.meta.schema.views.GridView;
import com.axelor.meta.schema.views.Notebook;
import com.axelor.meta.schema.views.Search;
import com.axelor.meta.schema.views.SimpleContainer;
import com.axelor.meta.service.MetaService;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
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
	
	@Inject
	private JpaSecurity security;
	
	private Class<?> findClass(String name) {
		try {
			return Class.forName(name);
		} catch (Exception e) {
		}
		return null;
	}

	@GET
	@Path("models")
	@SuppressWarnings("all")
	public Response models() {

		final Response response = new Response();
		final List<String> all = Lists.newArrayList();
		
		for (Class<?> cls : JPA.models()) {
			if (security.isPermitted(AccessType.READ, (Class) cls)) {
				all.add(cls.getName());
			}
		}
		
		Collections.sort(all);

		response.setData(all);
		response.setTotal(all.size());
		response.setStatus(Response.STATUS_SUCCESS);
		return response;
	}

	@GET
	@Path("fields/{model}")
	@SuppressWarnings("all")
	public Response fields(@PathParam("model") String model) {
		final Response response = new Response();
		final Map<String, Object> meta = Maps.newHashMap();
		final Class<?> modelClass = findClass(model);
		final List<String> names = Lists.newArrayList();
		
		if (!security.isPermitted(AccessType.READ, (Class) modelClass)) {
			response.setStatus(Response.STATUS_FAILURE);
			return response;
		}
		
		for (Property p : Mapper.of(modelClass).getProperties()) {
			if (!p.isTransient()) {
				names.add(p.getName());
			}
		}

		meta.put("model", model);
		meta.putAll(MetaStore.findFields(modelClass, names));

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

	private List<String> findNames(final List<String> names, final AbstractWidget widget) {
		List<? extends AbstractWidget> all = null;
		if (widget instanceof Notebook) {
			all = ((Notebook) widget).getPages();
		} else if (widget instanceof SimpleContainer) {
			all = ((SimpleContainer) widget).getItems();
		} else if (widget instanceof FormInclude) {
			names.addAll(findNames(((FormInclude) widget).getView()));
		} else if (widget instanceof Field) {
			names.add(((Field) widget).getName());
		}
		if (all == null) {
			return names;
		}
		for (AbstractWidget item : all) {
			findNames(names, item);
		}
		return names;
	}

	public List<String> findNames(final AbstractView view) {
		List<String> names = Lists.newArrayList();
		List<AbstractWidget> items = null;
		if (view instanceof FormView) {
			items = ((FormView) view).getItems();
		}
		if (view instanceof GridView) {
			items = ((GridView) view).getItems();
		}
		if (items == null || items.isEmpty()) {
			return names;
		}
		for (AbstractWidget widget : items) {
			findNames(names, widget);
		}
		return names;
	}

	@GET
	@Path("view")
	public Response view(
			@QueryParam("model") String model,
			@QueryParam("name") String name,
			@QueryParam("type") String type) {

		final Response response = service.findView(model, name, type);
		final AbstractView view = (AbstractView) response.getData();

		final Map<String, Object> data = Maps.newHashMap();
		data.put("view", view);

		if (view instanceof Search && ((Search) view).getSearchForm() != null) {
			String searchForm = ((Search) view).getSearchForm();
			Response searchResponse = service.findView(null, searchForm, "form");
			data.put("searchForm", searchResponse.getData());
		}

		if (view instanceof AbstractView && !StringUtils.isBlank(model)) {
			data.putAll(MetaStore.findFields(findClass(model), findNames((AbstractView) view)));
		}

		response.setData(data);
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}

	@POST
	@Path("view/fields")
	public Response viewFields(Request request) {
		final Response response = new Response();
		response.setData(MetaStore.findFields(request.getBeanClass(), request.getFields()));
		return response;
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
