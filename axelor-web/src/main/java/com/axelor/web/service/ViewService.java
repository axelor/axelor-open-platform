/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.web.service;

import java.util.Collections;
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
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.AbstractWidget;
import com.axelor.meta.schema.views.Field;
import com.axelor.meta.schema.views.FormInclude;
import com.axelor.meta.schema.views.FormView;
import com.axelor.meta.schema.views.GridView;
import com.axelor.meta.schema.views.Notebook;
import com.axelor.meta.schema.views.SimpleContainer;
import com.axelor.meta.service.MetaService;
import com.axelor.rpc.Request;
import com.axelor.rpc.Resource;
import com.axelor.rpc.Response;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
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

		final List<Permission> permissions = this.getPermissions(null);
		final Response response = new Response();
		if (permissions == null) {
			return Resource.models(new Request());
		}

		final List<String> all = Lists.newArrayList();
		final User user = AuthUtils.getUser();
		if (user.getGroup().getRestricted() == Boolean.TRUE) {
			for (Permission p : permissions) {
				if (p.getObject() == null || (p.getCanRead() != Boolean.TRUE && p.getReadCondition() == null)) {
					continue;
				}
				all.add(p.getObject());
			}
		} else {
			final List<String> exclude = Lists.newArrayList();
			for (Permission p : permissions) {
				if (p.getObject() == null || (p.getCanRead() != Boolean.TRUE && p.getReadCondition() == null)) {
					exclude.add(p.getObject());
				}
			}
			for (Class<?> cls : JPA.models()) {
				if (exclude.indexOf(cls.getName()) == -1) {
					all.add(cls.getName());
				}
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
	public Response fields(@PathParam("model") String model) {
		final Response response = new Response();
		final Map<String, Object> meta = Maps.newHashMap();
		final Class<?> modelClass = findClass(model);
		final List<Object> fields = Lists.newArrayList();
		final List<Permission> permissions = this.getPermissions(model);

		if (permissions != null) {
			final User user = AuthUtils.getUser();
			final Permission perm = permissions.isEmpty() ? null : permissions.get(0);
			if (perm == null && user.getGroup().getRestricted() == Boolean.TRUE) {
				response.setStatus(Response.STATUS_FAILURE);
				return response;
			}
			if (perm != null && perm.getCanRead() != Boolean.TRUE && perm.getReadCondition() == null) {
				response.setStatus(Response.STATUS_FAILURE);
				return response;
			}
		}

		for (Property p : Mapper.of(modelClass).getProperties()) {
			Map<String, Object> map = p.toMap();
			String title = p.getTitle();
			if (title == null) {
				title = p.getName();
			}
			title = JPA.translate(p.getName(), p.getTitle(), modelClass.getName(), "field");
			map.put("title", title);
			fields.add(map);
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

	private Map<String, Object> findFields(final String model, final List<String> names) {
		final Map<String, Object> data = Maps.newHashMap();

		if (Strings.isNullOrEmpty(model)) {
			return data;
		}

		final Class<?> modelClass = findClass(model);
		final Mapper mapper = Mapper.of(modelClass);
		final List<Object> fields = Lists.newArrayList();

		boolean massUpdate = false;

		for(String name : names) {
			Property p = findField(mapper, name);
			if (p != null) {
				Map<String, Object> map = p.toMap();
				map.put("name", name);
				if (p.getSelection() != null && !"".equals(p.getSelection().trim())) {
					map.put("selection", p.getSelection());
					map.put("selectionList", findSelection(p));
				}
				if (p.getTarget() != null) {
					map.put("perms", perms(p.getTarget()));
				}
				if (p.isMassUpdate()) {
					massUpdate = true;
				}
				fields.add(map);
			}
		}

		Map<String, Object> perms = this.perms(modelClass);

		if (massUpdate) {
			if (perms == null) {
				perms = Maps.newHashMap();
			}
			perms.put("massUpdate", massUpdate);
		}

		data.put("perms", perms);
		data.put("fields", fields);

		return data;
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

		if (view instanceof AbstractView) {
			data.putAll(findFields(model, findNames((AbstractView) view)));
		}

		response.setData(data);
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}

	@POST
	@Path("view/fields")
	public Response viewFields(Request request) {
		final Response response = new Response();
		response.setData(findFields(request.getModel(), request.getFields()));
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
		map.put("export", p.getCanExport());

		return map;
	}

	private List<Permission> getPermissions(String model) {
		final User user = AuthUtils.getUser();
		if (user == null || user.getGroup() == null
				|| "admin".equals(user.getCode())
				|| "admins".equals(user.getGroup().getCode())) {
			return null;
		}

		String s = "SELECT p FROM User u " +
				"LEFT JOIN u.group AS g " +
				"LEFT JOIN g.permissions AS p " +
				"WHERE u.code = :code";

		if (model == null) {
			s += " AND p.object IS NOT NULL";
		} else {
			s += " AND p.object = :obj";
		}

		TypedQuery<Permission> q = JPA.em().createQuery(s, Permission.class);

		q.setParameter("code", user.getCode());
		if (model != null) {
			q.setParameter("obj", model);
		}

		return q.getResultList();
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
		List<MetaSelectItem> items = MetaSelectItem.all().filter("self.select.id = ?", select.getId()).order("order").fetch();
		if (items == null || items.isEmpty()) {
			return null;
		}
		for(MetaSelectItem item : items) {
			all.add(ImmutableMap.of("value", item.getValue(), "title", JPA.translate(item.getTitle(), item.getTitle(), null, "select")));
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
