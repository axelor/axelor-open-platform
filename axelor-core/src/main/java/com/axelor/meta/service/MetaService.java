/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.FileUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.QueryBinder;
import com.axelor.db.mapper.Mapper;
import com.axelor.meta.db.MetaActionMenu;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaAttachmentRepository;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.ChartView;
import com.axelor.meta.schema.views.ChartView.ChartConfig;
import com.axelor.meta.schema.views.ChartView.ChartSeries;
import com.axelor.meta.schema.views.MenuItem;
import com.axelor.meta.schema.views.Search;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
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
	private MetaFileRepository files;
	
	@Inject
	private MetaAttachmentRepository attachments;

	@SuppressWarnings("unchecked")
	private List<MenuItem> findMenus(Query query) {

		QueryBinder.of(query).setCacheable();

		List<MenuItem> menus = Lists.newArrayList();
		List<Object[]> all = query.getResultList();

		for(Object[] items : all) {

			MetaMenu menu = (MetaMenu) items[0];
			MenuItem item = new MenuItem();
			item.setName(menu.getName());
			item.setPriority(menu.getPriority());
			item.setTitle(menu.getTitle());
			item.setIcon(menu.getIcon());
			item.setTop(menu.getTop());
			item.setLeft(menu.getLeft());
			item.setMobile(menu.getMobile());

			if (menu.getParent() != null) {
				item.setParent(menu.getParent().getName());
			}

			if (menu.getAction() != null) {
				item.setAction(menu.getAction().getName());
			}

			menus.add(item);
		}

		return menus;
	}

	public List<MenuItem> getMenus() {

		User user = AuthUtils.getUser();

		String q1 = "SELECT self, COALESCE(self.priority, 0) AS priority FROM MetaMenu self LEFT JOIN self.groups g WHERE ";
		Object p1 = null;

		if (user != null && user.getGroup() != null) {
			p1 = user.getGroup().getCode();
		}

		if (p1 != null) {
			q1 += "(g.code = ?1 OR self.groups IS EMPTY)";
		} else {
			q1 += "self.groups IS EMPTY";
		}

		q1 += " ORDER BY priority DESC, self.id";

		Query query = JPA.em().createQuery(q1);
		if (p1 != null)
			query.setParameter(1, p1);

		return findMenus(query);
	}

	public List<MenuItem> getMenus(String parent) {

		User user = AuthUtils.getUser();

		String q1 = "SELECT self, COALESCE(self.priority, 0) AS priority FROM MetaMenu self LEFT JOIN self.groups g WHERE ";
		String q2 = "self.parent IS NULL";
		Object p1 = null;
		Object p2 = null;

		if (user != null && user.getGroup() != null) {
			p2 = user.getGroup().getCode();
		}

		if (Strings.isNullOrEmpty(parent) || "null".endsWith(parent)) {
			q1 += q2;
		} else {
			q1 += "self.parent.name = ?1";
			p1 = parent;
		}

		if (p2 != null) {
			q1 += " AND (g.code = ?2 OR self.groups IS EMPTY)";
		} else {
			q1 += " AND self.groups IS EMPTY";
		}

		q1 += " ORDER BY priority DESC, self.id";

		Query query = JPA.em().createQuery(q1);
		if (p1 != null)
			query.setParameter(1, p1);
		if (p2 != null)
			query.setParameter(2, p2);

		return findMenus(query);
	}

	public List<MenuItem> getActionMenus(String parent, String category) {

		if ("null".equals(parent))
			parent = null;
		if ("null".equals(category))
			category = null;

		String str = "SELECT self FROM MetaActionMenu self WHERE self.parent.name = ?1";
		if (Strings.isNullOrEmpty(parent)) {
			str = "SELECT self FROM MetaActionMenu self WHERE self.parent IS NULL";
		}
		if (!Strings.isNullOrEmpty(category)) {
			str += " AND self.category = ?2";
		}
		str += " ORDER BY self.name";

		TypedQuery<MetaActionMenu> query = JPA.em().createQuery(str, MetaActionMenu.class);
		if (!Strings.isNullOrEmpty(parent)) {
			query.setParameter(1, parent);
		}
		if (!Strings.isNullOrEmpty(category)) {
			query.setParameter(2, category);
		}

		QueryBinder.of(query).setCacheable();

		List<MenuItem> menus = Lists.newArrayList();
		List<MetaActionMenu> all = query.getResultList();

		for(MetaActionMenu menu : all) {

			MenuItem item = new MenuItem();
			item.setName(menu.getName());
			item.setTitle(menu.getTitle());

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

		return menus;
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

		AbstractView data = XMLViews.findView(model, name, type);
		response.setData(data);
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

		Search search = (Search) XMLViews.findView(null, name, "search");
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

	@SuppressWarnings("unchecked")
	public Response getAttachment(long id, String model, Request request){
		Response response = new Response();
		List<MetaFile> data = Lists.newArrayList();
		List<String> fields = request.getFields();

		StringBuilder map = new StringBuilder();
		map.append("meta.id as id, meta.version as version");
		for (String fielName : fields) {
			if(fielName.equals("id") || fielName.equals("version")){
				continue;
			}
			map.append(", meta." + fielName + " as " + fielName);
		}

		javax.persistence.Query query = JPA.em().createQuery(
				"SELECT new map(" + map.toString() + ") FROM MetaFile meta " +
				"WHERE meta.id IN (SELECT attch.metaFile FROM MetaAttachment attch WHERE attch.objectName = ?1 AND attch.objectId = ?2)");
		query.setParameter(1, model);
		query.setParameter(2, id);

		data = query.getResultList();
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

		ChartView chart = (ChartView) XMLViews.findView(null, name, "chart");
		if (chart == null) {
			return response;
		}

		final Map<String, Object> data = Maps.newHashMap();
		
		response.setData(data);
		response.setStatus(Response.STATUS_SUCCESS);
		
		boolean hasOnInit = !StringUtils.isBlank(chart.getOnInit());
		boolean hasDataSet = request.getFields() != null && request.getFields().contains("dataset");
		
		if (hasDataSet || !hasOnInit) {
			
			final String string = chart.getQuery().getText();
			final Query query = "sql".equals(chart.getQuery().getType()) ?
					JPA.em().createNativeQuery(string) :
					JPA.em().createQuery(string);
	
			// return result as list of map
			((org.hibernate.ejb.QueryImpl<?>) query).getHibernateQuery()
				.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
	
			final Map<String, Object> context = Maps.newHashMap();
			if (request.getData() != null) {
				context.putAll(request.getData());
			}
			if (AuthUtils.getUser() != null) {
				context.put("__user__", AuthUtils.getUser());
				context.put("__userId__", AuthUtils.getUser().getId());
				context.put("__userCode__", AuthUtils.getSubject());
			}
	
			if (request.getData() != null) {
				QueryBinder.of(query).bind(context);
			}

			data.put("dataset", query.getResultList());
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
}
