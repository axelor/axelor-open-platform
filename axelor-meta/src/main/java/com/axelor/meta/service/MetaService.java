package com.axelor.meta.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.QueryBinder;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.GroovyScriptHelper;
import com.axelor.meta.MetaLoader;
import com.axelor.meta.db.MetaActionMenu;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaChart;
import com.axelor.meta.db.MetaChartSeries;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.views.AbstractView;
import com.axelor.meta.views.Action;
import com.axelor.meta.views.ActionMenuItem;
import com.axelor.meta.views.MenuItem;
import com.axelor.meta.views.Search;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class MetaService {
	
	private static final Logger LOG = LoggerFactory.getLogger(MetaService.class);
	private String QUOTE = "\"";
	private String COMMA = ",";
	
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

		Search search = (Search) loader.findView(null, name, "search");
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
	public Response removeAttachment(Request request) {
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
				com.axelor.db.Query<MetaAttachment> removedAtt = com.axelor.db.Query.of(MetaAttachment.class);
				removedAtt.filter("self.metaFile.id = ?1", fileId).delete();
				
				com.axelor.db.Query<MetaFile> removedFile = com.axelor.db.Query.of(MetaFile.class);
				removedFile.filter("self.id = ?", fileId).delete();
				
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
	
	@SuppressWarnings("unchecked")
	public void exportTranslations(String path) throws IOException {
		
		Query query = JPA.em().createNativeQuery("SELECT distinct language from meta_translation");
		
		List<String> languageList = query.getResultList();
		List<String> header = Lists.newArrayList();
		String headerLine = createHeader(header);
		path = path.endsWith("/") ? path : path.concat("/");
		for (String language : languageList) {
			File output = new File(path + language + ".csv");
			String contents = createCSV(language, header, headerLine);
			Files.createParentDirs(output);
			Files.write(contents, output, Charsets.UTF_8);
		}

	}
	
	private String createCSV(String language, List<String> header, String headerLine){
		
		StringBuilder sb = new StringBuilder(headerLine);
		
		List<MetaTranslation> metaList = MetaTranslation.all().filter("self.language = ?1", language).fetch();
		
		for (MetaTranslation metaTranslation : metaList) {
			
			boolean first = true;
			Map<String, Object> values = Mapper.toMap(metaTranslation);
			
			for (String column : header) {
				String value = (String) values.get(column);
				if(!first)
					sb.append(COMMA);
				
				first = false;
				
				if(value != null) 
					sb.append(QUOTE).append(value).append(QUOTE);
				
			}
			
			sb.append("\n");
		}
		
		sb.replace(sb.length()-1, sb.length(), "");
		return sb.toString();
		
	}
	
	private String createHeader(List<String> header){
		
		StringBuilder sb = new StringBuilder();
		
		//Ignore these fields for export
		Pattern pattern = Pattern.compile("(id|selected|createdOn|createdBy|archived|updatedOn|version|updatedBy|language)", Pattern.CASE_INSENSITIVE);
		boolean first = true;
		
		Mapper mapper = Mapper.of(MetaTranslation.class);
		for (Property property : mapper.getProperties()) {
			
			if(pattern.matcher(property.getName()).matches()){
				continue;
			}
			
			if(!first)
				sb.append(COMMA);
			
			first = false;
			sb.append(QUOTE).append(property.getName()).append(QUOTE);
			header.add(property.getName());
			
		}

		return sb.toString();
	}
	
	public Response getChart(final String name, final Request request) {
		
		final Response response = new Response();
		final MetaChart chart = MetaChart.all().filter("self.name = ?", name).fetchOne();
		
		if (chart == null || chart.getChartSeries() == null) {
			return response;
		}

		final Map<String, Object> data = Maps.newHashMap();

		data.put("title", chart.getTitle());
		data.put("stacked", chart.getStacked());
		
		data.put("xAxis", chart.getCategoryKey());
		data.put("xType", chart.getCategoryType());
		data.put("xTitle", chart.getCategoryTitle());
		
		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {
				
				String string = chart.getQuery();
				Query query = "sql".equals(chart.getQueryType()) ?
						JPA.em().createNativeQuery(string) :
						JPA.em().createQuery(string);

				// return result as list of map
				((org.hibernate.ejb.QueryImpl<?>) query).getHibernateQuery()
					.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

				Map<String, Object> context = Maps.newHashMap();
				if (request.getData() != null) {
					context.putAll(request.getData());
				}
				if (AuthUtils.getUser() != null) {
					context.put("__user__", AuthUtils.getUser());
					context.put("__userId__", AuthUtils.getUser().getId());
					context.put("__userCode__", AuthUtils.getSubject());
				}

				if (request.getData() != null) {
					QueryBinder binder = new QueryBinder(query);
					binder.bind(context, null);
				}

				data.put("dataset", query.getResultList());
			}
		});

		List<Object> series = Lists.newArrayList();
		for(MetaChartSeries s : chart.getChartSeries()) {
			Map<String, Object> map = Maps.newHashMap();
			map.put("key", s.getKey());
			map.put("type", s.getType());
			map.put("groupBy", s.getGroupBy());
			map.put("aggregate", s.getAggregate());
			map.put("title", s.getTitle());
			series.add(map);
		}
		data.put("series", series);

		response.setData(data);
		
		response.setStatus(Response.STATUS_SUCCESS);
		return response;
	}
}
