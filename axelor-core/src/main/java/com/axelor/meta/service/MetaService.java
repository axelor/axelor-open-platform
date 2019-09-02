/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

import static com.axelor.common.StringUtils.isBlank;
import static com.axelor.meta.loader.ModuleManager.isInstalled;

import com.axelor.app.internal.AppFilter;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.QueryBinder;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.meta.ActionExecutor;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaActionMenu;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.MetaViewCustom;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.axelor.meta.db.repo.MetaHelpRepository;
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
import com.axelor.rpc.filter.Filter;
import com.axelor.rpc.filter.JPQLFilter;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;
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
import javax.persistence.TypedQuery;
import org.hibernate.query.internal.AbstractProducedQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaService {

  private static final Logger LOG = LoggerFactory.getLogger(MetaService.class);

  @Inject private MetaViewRepository views;

  @Inject private MetaViewCustomRepository customViews;

  @Inject private MetaFileRepository files;

  @Inject private MetaFiles metaFiles;

  @Inject private ActionExecutor actionExecutor;

  private boolean canShow(
      MenuItem item, Map<String, MenuItem> map, Set<String> visited, ScriptHelper helper) {
    if (visited == null) {
      visited = new HashSet<>();
    }
    if (visited.contains(item.getName())) {
      LOG.warn("Recursion detected at menu: " + item.getName());
      return false;
    }
    visited.add(item.getName());
    if (item.getHidden() == Boolean.TRUE || !test(item, helper)) {
      return false;
    }
    if (item.getParent() == null) {
      return true;
    }
    final MenuItem parent = map.get(item.getParent());
    if (parent == null) {
      return false;
    }
    return canShow(parent, map, visited, helper);
  }

  private boolean test(MenuItem item, ScriptHelper helper) {
    final String module = item.getModuleToCheck();
    final String condition = item.getConditionToCheck();
    if (!isBlank(module) && !isInstalled(module)) {
      return false;
    }
    if (isBlank(condition)) {
      return true;
    }
    return helper.test(condition);
  }

  private List<MenuItem> filter(List<MenuItem> items) {

    final Map<String, MenuItem> map = new LinkedHashMap<>();
    final Set<String> visited = new HashSet<>();
    final List<MenuItem> all = new ArrayList<>();

    final Map<String, Object> vars = new HashMap<>();
    final ScriptHelper scriptHelper = new CompositeScriptHelper(new ScriptBindings(vars));

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
      if (canShow(item, map, null, scriptHelper)) {
        all.add(item);
      }
    }

    Collections.sort(
        all,
        new Comparator<MenuItem>() {

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

  @SuppressWarnings("all")
  private String getTag(MetaMenu item) {

    final String tag = item.getTag();
    final String call = item.getTagGet();
    final MetaAction action = item.getAction();

    if (tag != null) {
      return tag;
    }
    if (call != null) {
      final ActionRequest request = new ActionRequest();
      request.setAction(call);
      try {
        return (String) actionExecutor.execute(request).getItem(0);
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
      try {
        final Map<String, Object> data =
            (Map) ((Map) actionExecutor.execute(request).getItem(0)).get("view");
        final Map<String, Object> context = (Map) data.get("context");
        final String domain = (String) data.get("domain");
        final List<Filter> filters =
            Lists.newArrayList(new JPQLFilter("self.archived IS NULL OR self.archived = FALSE"));
        if (StringUtils.notBlank(domain)) {
          filters.add(JPQLFilter.forDomain(domain));
        }
        final Filter filter = Filter.and(filters);
        return String.valueOf(filter.build((Class) request.getBeanClass()).bind(context).count());
      } catch (Exception e) {
        LOG.error("Unable to read tag for menu: {}", item.getName());
        LOG.trace("Error", e);
      }
    }

    return null;
  }

  public List<MenuItem> getMenus(boolean withTagsOnly) {

    // make sure to apply hot updates
    if (!withTagsOnly) {
      XMLViews.applyHotUpdates();
    }

    final User user = AuthUtils.getUser();

    if (user == null) {
      return Collections.emptyList();
    }

    final Map<Long, Set<String>> menuGroups = new HashMap<>();
    final Map<Long, Set<String>> menuRoles = new HashMap<>();

    final Query permsQuery =
        JPA.em()
            .createQuery(
                "SELECT new List(m.id, g.code, r.name) "
                    + "FROM MetaMenu m "
                    + "LEFT JOIN m.groups g "
                    + "LEFT JOIN m.roles r");
    QueryBinder.of(permsQuery).setCacheable();

    // prepare group, roles info to avoid additional queries
    for (Object item : permsQuery.getResultList()) {
      final List<?> vals = (List<?>) item;
      final Long id = (Long) vals.get(0);
      if (vals.get(1) != null) {
        Set<String> groups = menuGroups.get(id);
        if (groups == null) {
          groups = new HashSet<>();
          menuGroups.put(id, groups);
        }
        groups.add(vals.get(1).toString());
      }
      if (vals.get(2) != null) {
        Set<String> roles = menuRoles.get(id);
        if (roles == null) {
          roles = new HashSet<>();
          menuRoles.put(id, roles);
        }
        roles.add(vals.get(2).toString());
      }
    }

    final StringBuilder queryString =
        new StringBuilder()
            .append("SELECT self FROM MetaMenu self ")
            .append("LEFT JOIN FETCH self.action ")
            .append("LEFT JOIN FETCH self.parent ")
            .append(withTagsOnly ? "WHERE (self.tag IS NOT NULL OR self.tagGet IS NOT NULL) " : "")
            .append(" ORDER BY COALESCE(self.priority, 0) DESC, self.id");

    final TypedQuery<MetaMenu> query = JPA.em().createQuery(queryString.toString(), MetaMenu.class);
    QueryBinder.of(query).setCacheable();

    final List<MenuItem> menus = new ArrayList<>();
    final List<MetaMenu> records = new ArrayList<>();

    for (MetaMenu menu : query.getResultList()) {
      records.add(menu);
      while (withTagsOnly && menu.getParent() != null) {
        // need to get parents to check visibility
        menu = menu.getParent();
        records.add(menu);
      }
    }

    final String userGroup = user.getGroup() == null ? null : user.getGroup().getCode();
    final List<String> userRoles = new ArrayList<>();
    if (user.getRoles() != null) {
      for (Role role : user.getRoles()) {
        userRoles.add(role.getName());
      }
    }
    if (user.getGroup() != null && user.getGroup().getRoles() != null) {
      for (Role role : user.getGroup().getRoles()) {
        userRoles.add(role.getName());
      }
    }

    final Map<String, String> help = new HashMap<>();
    if (!withTagsOnly && user.getNoHelp() != Boolean.TRUE) {
      final MetaHelpRepository helpRepo = Beans.get(MetaHelpRepository.class);
      final String lang =
          AppFilter.getLocale() == null ? "en" : AppFilter.getLocale().getLanguage();
      helpRepo
          .all()
          .filter("self.menu is not null and self.language = :lang")
          .bind("lang", lang)
          .cacheable()
          .select("menu", "help")
          .fetch(-1, 0)
          .forEach(
              item -> {
                help.put((String) item.get("menu"), (String) item.get("help"));
              });
    }

    final Set<String> denied = new HashSet<>();

    for (final MetaMenu menu : records) {
      // check for user menus
      if (menu.getUser() != null && menu.getUser() != user) {
        continue;
      }
      // if no group access, check for roles
      final Set<String> myGroups = menuGroups.get(menu.getId());
      final Set<String> myRoles = menuRoles.get(menu.getId());

      boolean allowed =
          AuthUtils.isAdmin(user)
              || (myGroups != null && myGroups.contains(userGroup))
              || (myRoles != null && !Collections.disjoint(userRoles, myRoles))
              || (myRoles == null && myGroups == null && menu.getParent() != null);

      if (!allowed || denied.contains(menu.getName())) {
        denied.add(menu.getName());
        continue;
      }

      final MenuItem item = new MenuItem();
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
      item.setModuleToCheck(menu.getModuleToCheck());
      item.setConditionToCheck(menu.getConditionToCheck());

      if (help.containsKey(menu.getName())) {
        item.setHelp(help.get(menu.getName()));
      }

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

  @SuppressWarnings("unchecked")
  public List<MenuItem> getActionMenus(String parent, String category) {

    if ("null".equals(parent)) parent = null;
    if ("null".equals(category)) category = null;

    String str =
        "SELECT self, COALESCE(self.priority, 0) AS priority FROM MetaActionMenu self WHERE self.parent.name = ?1";
    if (Strings.isNullOrEmpty(parent)) {
      str =
          "SELECT self, COALESCE(self.priority, 0) AS priority FROM MetaActionMenu self WHERE self.parent IS NULL";
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

    for (Object[] items : all) {

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

    MetaViewCustom entity = customViews.findByUser(view.getName(), user);
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

  @Transactional
  public int removeCustomViews(MetaView view) {
    if (view == null || StringUtils.isBlank(view.getName())) {
      return 0;
    }
    Query deleteQuery =
        JPA.em().createQuery("DELETE FROM MetaViewCustom self WHERE self.name = :name");
    deleteQuery.setParameter("name", view.getName());
    return deleteQuery.executeUpdate();
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

    for (Search.SearchSelect select : search.getSelects()) {

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

      for (Object item : items) {
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

  public Response getAttachment(long id, String model, Request request) {
    Response response = new Response();
    List<String> fields = request.getFields();

    com.axelor.db.Query<MetaFile> query =
        JPA.all(MetaFile.class)
            .filter(
                "self.id IN (SELECT a.metaFile FROM MetaAttachment a WHERE a.objectName = :model AND a.objectId = :id)");

    query.bind("model", model);
    query.bind("id", id);

    Object data = query.select(fields.toArray(new String[] {})).fetch(-1, -1);

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
      return response.fail("No records provides.");
    }

    for (Object record : records) {
      @SuppressWarnings("all")
      Long fileId = Long.valueOf(((Map) record).get("id").toString());

      if (fileId != null) {
        MetaFile obj = files.find(fileId);
        try {
          metaFiles.delete(obj);
          result.add(record);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
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
    JPA.manage((Model) attBean);

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

        res = actionExecutor.execute(req);

        data.put("dataset", res.getData());

      } else {
        Query query =
            "sql".equals(chart.getDataSet().getType())
                ? JPA.em().createNativeQuery(string)
                : JPA.em().createQuery(string);

        // return result as list of map
        this.transformQueryResult(query);

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

    for (ChartSeries cs : chart.getSeries()) {
      Map<String, Object> map = Maps.newHashMap();
      map.put("key", cs.getKey());
      map.put("type", cs.getType());
      map.put("groupBy", cs.getGroupBy());
      map.put("aggregate", cs.getAggregate());
      map.put("title", cs.getLocalizedTitle());
      series.add(map);
    }

    if (chart.getConfig() != null) {
      for (ChartConfig c : chart.getConfig()) {
        config.put(c.getName(), c.getValue());
      }
    }

    data.put("series", series);
    data.put("config", config);
    data.put("search", chart.getSearchFields());
    data.put("onInit", chart.getOnInit());

    if ("sql".equals(chart.getDataSet().getType())) {
      data.put("usingSQL", true);
    }

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

      res = actionExecutor.execute(req);

      data.put("dataset", res.getData());
    } else {
      Query query =
          "sql".equals(report.getDataSet().getType())
              ? JPA.em().createNativeQuery(dataSet.getText())
              : JPA.em().createQuery(dataSet.getText());

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
      this.transformQueryResult(query);

      if (request.getData() != null) {
        QueryBinder.of(query).bind(context);
      }

      data.put("dataset", query.getResultList());
    }

    return response;
  }

  private void transformQueryResult(Query query) {
    // TODO: fix deprecation when new transformer api is implemented in hibernate
    ((AbstractProducedQuery<?>) query)
        .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
  }
}
