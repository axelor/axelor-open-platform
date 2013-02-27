package com.axelor.meta.service;

import java.util.List;
import java.util.Map;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.meta.GroovyScriptHelper;
import com.axelor.meta.MetaLoader;
import com.axelor.meta.db.MetaActionMenu;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.views.AbstractView;
import com.axelor.meta.views.Action;
import com.axelor.meta.views.ActionMenuItem;
import com.axelor.meta.views.MenuItem;
import com.axelor.meta.views.Search;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class MetaService {
	
	private static final Logger LOG = LoggerFactory.getLogger(MetaService.class);
	
	@Inject
	private MetaLoader loader;
	
	@SuppressWarnings("unchecked")
	private List<MenuItem> findMenus(Query query) {
		
		List<MenuItem> menus = Lists.newArrayList();
		List<Object[]> all = query.getResultList();
		
		for(Object[] items : all) {

			MetaMenu menu = (MetaMenu) items[0];
			MenuItem item = new MenuItem();
			item.setName(menu.getName());
			item.setPriority(menu.getPriority());
			item.setTitle(menu.getTitle());
			item.setIcon(menu.getIcon());
			
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
	
	public List<MenuItem> getMenus(String parent) {
		
		Subject subject = SecurityUtils.getSubject();
		User user = null;
		
		if (subject != null) {
			user = User.all().filter("self.code = ?1", subject.getPrincipal()).fetchOne();
		}
		
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

		List<MenuItem> menus = Lists.newArrayList();
		List<MetaActionMenu> all = query.getResultList();
		
		for(MetaActionMenu menu : all) {

			ActionMenuItem item = new ActionMenuItem();
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
		return loader.findAction(name);
	}
	
	public Response findViews(Class<?> model, Map<String, String> views) {
		Response response = new Response();

		Map<String, Object> data = loader.findViews(model.getName(), views);
		response.setData(data);
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}
	
	public Response findView(String model, String name, String type) {
		Response response = new Response();

		AbstractView data = loader.findView(model, name, type);
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

		Search search = (Search) loader.findView(name);
		GroovyScriptHelper helper = search.scriptHandler(context);

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
					((Map) item).put("_modelTitle", select.getTitle());
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
}
