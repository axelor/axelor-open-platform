/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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
package com.axelor.meta.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.Query;

import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.common.FileUtils;
import com.axelor.common.ObjectUtils;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.axelor.db.QueryBinder;
import com.axelor.db.mapper.Mapper;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaActionMenu;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.MetaViewCustom;
import com.axelor.meta.db.repo.MetaAttachmentRepository;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.axelor.meta.db.repo.MetaViewCustomRepository;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.ChartView;
import com.axelor.meta.schema.views.ChartView.ChartConfig;
import com.axelor.meta.schema.views.ChartView.ChartSeries;
import com.axelor.meta.schema.views.CustomView;
import com.axelor.meta.schema.views.DataSet;
import com.axelor.meta.schema.views.MenuItem;
import com.axelor.meta.schema.views.Search;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

public class MetaService {

	private static final Logger LOG = LoggerFactory.getLogger(MetaService.class);
	
	@Inject
	private MetaViewRepository views;
	
	@Inject
	private MetaViewCustomRepository customViews;

	@Inject
	private MetaFileRepository files;
	
	@Inject
	private MetaAttachmentRepository attachments;

	private boolean canShow(MenuItem item, Map<String, MenuItem> map, Set<String> visited) {
		if (visited == null) {
			visited = new HashSet<>();
		}
		if (visited.contains(item.getName())) {
			LOG.warn("Recursion detected at menu: " + item.getName());
			return false;
		}
		visited.add(item.getName());
		if (item.getHidden() == Boolean.TRUE) {
			return false;
		}
		if (item.getParent() == null) {
			return true;
		}
		final MenuItem parent = map.get(item.getParent());
		if (parent == null) {
			return false;
		}
		return canShow(parent, map, visited);
	}

	private List<MenuItem> filter(List<MenuItem> items) {

		final Map<String, MenuItem> map = new LinkedHashMap<>();
		final Set<String> visited = new HashSet<>();
		final List<MenuItem> all = new ArrayList<>();

		for (MenuItem item : items) {
			final String name = item.getName();
			if (visited.contains(name)) {
				continue;
			}
			visited.add(name);
			if (item.getHidden() != Boolean.TRUE) {
				map.put(name, item);
			}
		}

		for (final String name : map.keySet()) {
			final MenuItem item = map.get(name);
			if (canShow(item, map, null)) {
				all.add(item);
			}
		}

		Collections.sort(all, new Comparator<MenuItem>() {

			@Override
			public int compare(MenuItem a, MenuItem b) {
				Integer n = a.getOrder();
				Integer m = b.getOrder();

				if (n == null) n = 0;
				if (m == null) m = 0;

				return Integer.compare(n, m);
			}
		});

		return all;
	}

	private List<MenuItem> findMenus(Query query, boolean withTagsOnly) {

		QueryBinder.of(query).setCacheable();

		final List<MenuItem> menus = new ArrayList<>();
		final Set<Role> roles = new HashSet<>();
		final User user = AuthUtils.getUser();

		if (user != null && user.getRoles() != null) {
			roles.addAll(user.getRoles());
		}
		if (user != null && user.getGroup() != null && user.getGroup().getRoles() != null) {
			roles.addAll(user.getGroup().getRoles());
		}

		final List<MetaMenu> all = new ArrayList<>();

		for (Object tuple : query.getResultList()) {
			MetaMenu menu = (MetaMenu) ((Object[]) tuple)[0];
			all.add(menu);

			while (withTagsOnly && menu.getParent() != null) {
				// need to get parents to check visibility
				menu = menu.getParent();
				all.add(menu);
			}
		}

		for(final MetaMenu menu : all) {

			final MenuItem item = new MenuItem();

			// check user
			if (menu.getUser() != null && menu.getUser() != user) {
				continue;
			}

			boolean hasGroup =  !ObjectUtils.isEmpty(menu.getGroups()) && menu.getGroups().contains(user.getGroup());

			// if no group access, check for roles
			if (!hasGroup && !AuthUtils.isAdmin(user) && !ObjectUtils.isEmpty(menu.getRoles())) {
				boolean hasRole = false;
				for (final Role role : roles) {
					if (menu.getRoles().contains(role)) {
						hasRole = true;
						break;
					}
				}
				if (!hasRole) {
					continue;
				}
			}

			item.setName(menu.getName());
			item.setOrder(menu.getOrder());
			item.setTitle(menu.getTitle());
			item.setIcon(menu.getIcon());
			item.setIconBackground(menu.getIconBackground());
			item.setTag(getTag(menu));
			item.setTagStyle(menu.getTagStyle());
			item.setTop(menu.getTop());
			item.setLeft(menu.getLeft());
			item.setMobile(menu.getMobile());
			item.setHidden(menu.getHidden());

			if (menu.getParent() != null) {
				item.setParent(menu.getParent().getName());
			}

			if (menu.getAction() != null) {
				item.setAction(menu.getAction().getName());
			}

			menus.add(item);
		}

		return filter(menus);
	}

	@SuppressWarnings("all")
	private String getTag(MetaMenu item) {

		final String tag = item.getTag();
		final String call = item.getTagGet();
		final MetaAction action = item.getAction();

		if (tag != null) { return tag; }
		if (call != null) {
			final ActionRequest request = new ActionRequest();
			final ActionHandler handler = new ActionHandler(request);
			request.setAction(call);
			try {
				return (String) handler.execute().getItem(0);
			} catch (Exception e) {
				LOG.error("Unable to read tag for menu: {}", item.getName());
				LOG.trace("Error", e);
				return null;
			}
		}

		if (item.getTagCount() == Boolean.TRUE && action != null) {
			final ActionView act;
			try {
				act = (ActionView) MetaStore.getAction(action.getName());
			} catch (Exception e) {
				return null;
			}
			if (act == null) {
				return null;
			}
			final ActionRequest request = new ActionRequest();
			request.setAction(action.getName());
			request.setModel(action.getModel());
			request.setData(new HashMap<String, Object>());
			final ActionHandler handler = new ActionHandler(request);
			try {
				final Map<String, Object> data = (Map) ((Map) handler.execute().getItem(0)).get("view");
				final Map<String, Object> context = (Map) data.get("context");
				final String domain = (String) data.get("domain");
				final JpaRepository<?> repo = JpaRepository.of((Class) request.getBeanClass());
				return "" + (domain == null ?
						repo.all().count() :
						repo.all().filter(domain).bind(context).count());
			} catch (Exception e) {
				LOG.error("Unable to read tag for menu: {}", item.getName());
				LOG.trace("Error", e);
			}
		}

		return null;
	}

	public List<MenuItem> getMenus(boolean withTagsOnly) {

		final User user = AuthUtils.getUser();

		String qs = "SELECT self, COALESCE(self.priority, 0) AS priority FROM MetaMenu self LEFT JOIN self.groups g WHERE ";
		Object groupCode = null;

		if (user != null && user.getGroup() != null) {
			groupCode = user.getGroup().getCode();
		}

		if (groupCode != null) {
			qs += "(g.code = ?1 OR self.groups IS EMPTY) ";
		} else {
			qs += "self.groups IS EMPTY ";
		}

		if (withTagsOnly) {
			qs += "AND (self.tag IS NOT NULL OR self.tagGet IS NOT NULL) ";
		}

		qs += "ORDER BY priority DESC, self.id";

		Query query = JPA.em().createQuery(qs);
		if (groupCode != null) {
			query.setParameter(1, groupCode);
		}

		return findMenus(query, withTagsOnly);
	}

	@SuppressWarnings("unchecked")
	public List<MenuItem> getActionMenus(String parent, String category) {

		if ("null".equals(parent))
			parent = null;
		if ("null".equals(category))
			category = null;

		String str = "SELECT self, COALESCE(self.priority, 0) AS priority FROM MetaActionMenu self WHERE self.parent.name = ?1";
		if (Strings.isNullOrEmpty(parent)) {
			str = "SELECT self, COALESCE(self.priority, 0) AS priority FROM MetaActionMenu self WHERE self.parent IS NULL";
		}
		if (!Strings.isNullOrEmpty(category)) {
			str += " AND self.category = ?2";
		}
		str += " ORDER BY self.name, priority DESC";

		Query query = JPA.em().createQuery(str);
		if (!Strings.isNullOrEmpty(parent)) {
			query.setParameter(1, parent);
		}
		if (!Strings.isNullOrEmpty(category)) {
			query.setParameter(2, category);
		}

		QueryBinder.of(query).setCacheable();

		List<MenuItem> menus = new ArrayList<>();
		List<Object[]> all = query.getResultList();

		for(Object[] items : all) {

			MetaActionMenu menu = (MetaActionMenu) items[0];
			MenuItem item = new MenuItem();

			item.setName(menu.getName());
			item.setTitle(menu.getTitle());
			item.setOrder(menu.getOrder());
			item.setHidden(menu.getHidden());

			if (menu.getParent() != null) {
				item.setParent(menu.getParent().getName());
			}

			if (menu.getAction() != null) {
				item.setAction(menu.getAction().getName());
			}
			if (menu.getCategory() != null) {
				item.setCategory(menu.getCategory());
			}
			menus.add(item);
		}

		return filter(menus);
	}

	public Action getAction(String name) {
		return XMLViews.findAction(name);
	}

	public Response findViews(Class<?> model, Map<String, String> views) {
		Response response = new Response();

		Map<String, Object> data = XMLViews.findViews(model.getName(), views);
		response.setData(data);
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}

	public Response findView(String model, String name, String type) {
		Response response = new Response();

		AbstractView data = XMLViews.findView(name, type, model);
		response.setData(data);
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}

	@Transactional
	public Response saveView(AbstractView view, User user) {
		final Response response = new Response();
		final String xml = XMLViews.toXml(view, true);

		MetaViewCustom entity =  customViews.findByUser(view.getName(), user);
		if (entity == null) {
			entity = new MetaViewCustom();
			entity.setName(view.getName());
			entity.setType(view.getType());
			entity.setModel(view.getModel());
			entity.setUser(user);
		}

		entity.setTitle(view.getTitle());
		entity.setXml(xml);

		customViews.save(entity);

		response.setData(view);
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}

	@SuppressWarnings("all")
	public Response runSearch(Request request) {
		Response response = new Response();

		Map<String, Object> context = request.getData();
		String name = (String) context.get("__name");
		List<String> selected = (List) context.get("__selected");

		LOG.debug("Search : {}", name);

		Search search = (Search) XMLViews.findView(name, "search");
		ScriptHelper helper = search.scriptHandler(context);

		List<Object> data = Lists.newArrayList();

		for(Search.SearchSelect select : search.getSelects()) {

			if (selected != null && !selected.contains(select.getModel())) {
				continue;
			}

			LOG.debug("Model : {}", select.getModel());
			LOG.debug("Param : {}", context);

			Query query;
			try {
				query = select.toQuery(search, helper);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(e);
			}
			List<?> items = Lists.newArrayList();

			LOG.debug("Query : {}", select.getQueryString());

			if (query != null) {
				query.setFirstResult(request.getOffset());
				query.setMaxResults(search.getLimit());
				items = query.getResultList();
			}

			LOG.debug("Found : {}", items.size());

			for(Object item : items) {
				if (item instanceof Map) {
					((Map) item).put("_model", select.getModel());
					((Map) item).put("_modelTitle", select.getLocalizedTitle());
					((Map) item).put("_form", select.getFormView());
					((Map) item).put("_grid", select.getGridView());
				}
			}

			data.addAll(items);
		}

		LOG.debug("Total : {}", data.size());

		response.setData(data);
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}

	public Response getAttachment(long id, String model, Request request){
		Response response = new Response();
		List<String> fields = request.getFields();

		com.axelor.db.Query<MetaFile> query = JPA.all(MetaFile.class).filter(
				"self.id IN (SELECT a.metaFile FROM MetaAttachment a WHERE a.objectName = :model AND a.objectId = :id)");

		query.bind("model", model);
		query.bind("id", id);
		
		Object data = query.select(fields.toArray(new String[]{})).fetch(-1, -1);
		
		response.setData(data);
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}

	@Transactional
	public Response removeAttachment(Request request, String uploadPath) {
		Response response = new Response();
		List<Object> result = Lists.newArrayList();
		List<Object> records = request.getRecords();

		if (records == null || records.isEmpty()) {
			response.setException(new IllegalArgumentException("No records provides."));
			return response;
		}

		for(Object record : records) {
			@SuppressWarnings("all")
			Long fileId = Long.valueOf(((Map) record).get("id").toString());

			if (fileId != null) {
				MetaFile obj = files.find(fileId);
				if (uploadPath != null) {
					File file = FileUtils.getFile(uploadPath, obj.getFilePath());
					if (file.exists() && !file.delete()) {
						continue;
					}
				}

				attachments.all().filter("self.metaFile.id = ?1", fileId).delete();
				files.remove(obj);

				result.add(record);
			}
		}

		response.setData(result);
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}

	@Transactional
	public Response addAttachment(long id, Request request) {
		Response response = new Response();
		Map<String, Object> data = request.getData();
		Map<String, Object> map = Maps.newHashMap();

		Model fileBean = (Model) JPA.find(MetaFile.class, Long.valueOf(data.get("id").toString()));

		map.put("metaFile", fileBean);
		map.put("objectId", id);
		map.put("objectName", request.getModel());

		Object attBean = Mapper.toBean(MetaAttachment.class, map);
		JPA.manage( (Model) attBean);

		response.setData(attBean);
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}

	public Response getChart(final String name, final Request request) {

		final Response response = new Response();
		final MetaView view = views.findByName(name);
		
		if (view == null) {
			return response;
		}

		ChartView chart = (ChartView) XMLViews.findView(name, "chart");
		if (chart == null) {
			return response;
		}

		final Map<String, Object> data = new HashMap<>();
		
		response.setData(data);
		response.setStatus(Response.STATUS_SUCCESS);

		boolean hasDataSet = request.getFields() != null && request.getFields().contains("dataset");
		
		if (hasDataSet) {
			
			final String string = chart.getDataSet().getText();
			final Map<String, Object> context = Maps.newHashMap();
			if (request.getData() != null) {
				context.putAll(request.getData());
			}
			if (AuthUtils.getUser() != null) {
				context.put("__user__", AuthUtils.getUser());
				context.put("__userId__", AuthUtils.getUser().getId());
				context.put("__userCode__", AuthUtils.getUser().getCode());
			}

			if ("rpc".equals(chart.getDataSet().getType())) {
				ActionRequest req = new ActionRequest();
				ActionResponse res = new ActionResponse();
				Map<String, Object> reqData = new HashMap<>();

				reqData.put("context", context);

				req.setModel(request.getModel());
				req.setData(reqData);
				req.setAction(string);

				if (req.getModel() == null) {
					req.setModel(ScriptBindings.class.getName());
				}

				res = new ActionHandler(req).execute();

				data.put("dataset", res.getData());

			} else {
				Query query = "sql".equals(chart.getDataSet().getType()) ?
						JPA.em().createNativeQuery(string) :
						JPA.em().createQuery(string);

				// return result as list of map
				((org.hibernate.ejb.QueryImpl<?>) query).getHibernateQuery()
					.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

				if (request.getData() != null) {
					QueryBinder.of(query).bind(context);
				}

				data.put("dataset", query.getResultList());
			}
		}

		if (hasDataSet) {
			return response;
		}

		data.put("title", chart.getLocalizedTitle());
		data.put("stacked", chart.getStacked());

		data.put("xAxis", chart.getCategory().getKey());
		data.put("xType", chart.getCategory().getType());
		data.put("xTitle", chart.getCategory().getLocalizedTitle());

		List<Object> series = Lists.newArrayList();
		Map<String, Object> config = Maps.newHashMap();

		for(ChartSeries cs : chart.getSeries()) {
			Map<String, Object> map = Maps.newHashMap();
			map.put("key", cs.getKey());
			map.put("type", cs.getType());
			map.put("groupBy", cs.getGroupBy());
			map.put("aggregate", cs.getAggregate());
			map.put("title", cs.getLocalizedTitle());
			series.add(map);
		}

		if (chart.getConfig() != null) {
			for(ChartConfig c : chart.getConfig()) {
				config.put(c.getName(), c.getValue());
			}
		}

		data.put("series", series);
		data.put("config", config);
		data.put("search", chart.getSearchFields());
		data.put("onInit", chart.getOnInit());

		return response;
	}

	public Response getDataSet(final String viewName, final Request request) {

		final Response response = new Response();
		final MetaView metaView = views.findByName(viewName);

		if (metaView == null) {
			return response;
		}

		CustomView report = (CustomView) XMLViews.findView(viewName, "report");
		if (report == null) {
			return response;
		}

		final DataSet dataSet = report.getDataSet();
		final Map<String, Object> data = new HashMap<>();

		response.setData(data);
		response.setStatus(Response.STATUS_SUCCESS);

		final Map<String, Object> context = new HashMap<>();
		if (request.getData() != null) {
			context.putAll(request.getData());
		}
		if (AuthUtils.getUser() != null) {
			context.put("__user__", AuthUtils.getUser());
			context.put("__userId__", AuthUtils.getUser().getId());
			context.put("__userCode__", AuthUtils.getSubject());
		}

		if ("rpc".equals(dataSet.getType())) {
			ActionRequest req = new ActionRequest();
			ActionResponse res = new ActionResponse();

			req.setModel(request.getModel());
			req.setData(request.getData());
			req.setAction(dataSet.getText());

			if (req.getModel() == null) {
				req.setModel(ScriptBindings.class.getName());
			}

			res = new ActionHandler(req).execute();

			data.put("dataset", res.getData());
		} else {
			Query query = "sql".equals(report.getDataSet().getType()) ?
					JPA.em().createNativeQuery(dataSet.getText()) :
					JPA.em().createQuery(dataSet.getText());

			if (request.getLimit() > 0) {
				query.setMaxResults(request.getLimit());
			}
			if (request.getOffset() > 0) {
				query.setFirstResult(request.getOffset());
			}
			if (dataSet.getLimit() != null && dataSet.getLimit() > 0) {
				query.setMaxResults(dataSet.getLimit());
			}

			// return result as list of map
			((org.hibernate.ejb.QueryImpl<?>) query).getHibernateQuery()
				.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

			if (request.getData() != null) {
				QueryBinder.of(query).bind(context);
			}

			data.put("dataset", query.getResultList());
		}

		return response;
	}
}
