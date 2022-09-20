/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.meta.service;

import static com.axelor.common.StringUtils.isBlank;
import static com.axelor.meta.loader.ModuleManager.isInstalled;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.auth.db.ViewCustomizationPermission;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query.Selector;
import com.axelor.db.QueryBinder;
import com.axelor.db.mapper.Mapper;
import com.axelor.i18n.I18n;
import com.axelor.meta.ActionExecutor;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaActionMenu;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.MetaViewCustom;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.axelor.meta.db.repo.MetaViewCustomRepository;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.ChartView;
import com.axelor.meta.schema.views.ChartView.ChartAction;
import com.axelor.meta.schema.views.ChartView.ChartConfig;
import com.axelor.meta.schema.views.ChartView.ChartSeries;
import com.axelor.meta.schema.views.CustomView;
import com.axelor.meta.schema.views.DataSet;
import com.axelor.meta.schema.views.MenuItem;
import com.axelor.meta.schema.views.Search;
import com.axelor.meta.schema.views.Search.SearchSelectField;
import com.axelor.meta.service.menu.MenuItemComparator;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.hibernate.transform.BasicTransformerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaService {

  private static final Logger LOG = LoggerFactory.getLogger(MetaService.class);

  @Inject private MetaViewRepository views;

  @Inject private MetaViewCustomRepository customViews;

  @Inject private MetaFileRepository files;

  @Inject private MetaFiles metaFiles;

  @Inject private ActionExecutor actionExecutor;

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

  @SuppressWarnings({"unchecked", "JpaQlInspection"})
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

    final ScriptHelper scriptHelper =
        new CompositeScriptHelper(new ScriptBindings(new HashMap<>()));

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

      if (Boolean.TRUE.equals(item.getHidden()) || !test(item, scriptHelper)) {
        continue;
      }

      menus.add(item);
    }

    menus.sort(new MenuItemComparator());
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

    AbstractView data = XMLViews.findView(name, type, model);
    response.setData(data);
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  private ViewCustomizationPermission getViewCustomizationPermission(User user) {
    return Optional.ofNullable(user)
        .filter(u -> XMLViews.isCustomizationEnabled())
        .map(User::getGroup)
        .map(Group::getViewCustomizationPermission)
        .orElse(ViewCustomizationPermission.NOT_ALLOWED);
  }

  @Transactional
  public Response saveView(AbstractView view, User user) {
    return saveView(view, user, null);
  }

  @Transactional
  public Response saveView(AbstractView view, User user, Long customViewId) {
    final ViewCustomizationPermission viewCustomizationPermission =
        getViewCustomizationPermission(user);

    if (viewCustomizationPermission == ViewCustomizationPermission.NOT_ALLOWED) {
      throw new PersistenceException(I18n.get("You are not allowed to customize views."));
    }

    final Response response = new Response();
    final String xml = XMLViews.toXml(view, true);

    if (Objects.equals(view.getCustomViewShared(), Boolean.TRUE)
        && viewCustomizationPermission != ViewCustomizationPermission.CAN_SHARE) {
      throw new PersistenceException(I18n.get("You are not allowed to share custom views."));
    }

    if (viewCustomizationPermission == ViewCustomizationPermission.CAN_SHARE
        && customViewId != null) {
      view.setCustomViewId(customViewId);
    }

    MetaViewCustom entity =
        view.getCustomViewId() == null
            ? customViews.findByUser(view.getName(), user)
            : customViews.find(view.getCustomViewId());

    if (entity == null) {
      entity = new MetaViewCustom();
      entity.setName(view.getName());
      entity.setType(view.getType());
      entity.setModel(view.getModel());
      entity.setUser(user);
    }

    entity.setTitle(view.getTitle());
    entity.setXml(xml);
    entity.setShared(view.getCustomViewShared());

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

  @Transactional
  public int removeCustomViews(MetaView view, User user) {
    if (view == null || StringUtils.isBlank(view.getName()) || user == null) {
      return 0;
    }

    final ViewCustomizationPermission permission = getViewCustomizationPermission(user);

    if (permission == ViewCustomizationPermission.NOT_ALLOWED) {
      throw new PersistenceException(I18n.get("You are not allowed to customize views."));
    }

    int count =
        com.axelor.db.Query.of(MetaViewCustom.class)
            .filter("self.name = :name AND self.user = :user")
            .bind("name", view.getName())
            .bind("user", user)
            .delete();

    if (count == 0 && permission == ViewCustomizationPermission.CAN_SHARE) {
      count =
          com.axelor.db.Query.of(MetaViewCustom.class)
              .filter("self.name = :name AND self.shared = TRUE")
              .bind("name", view.getName())
              .delete();
    }

    return count;
  }

  static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
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

      Selector selector = select.toQuery(helper);

      if (selector == null) {
        LOG.debug("No query to run for {}", select.getModel());
        continue;
      }

      LOG.debug("Query : {}", selector.toString());
      int limit = search.getLimit();
      if (select.getLimit() != null && select.getLimit() > 0) {
        limit = select.getLimit();
      }

      List<?> items = selector.fetch(limit, request.getOffset());

      if (Objects.equals(Boolean.TRUE, select.getDistinct())) {
        items =
            items.stream()
                .filter(distinctByKey(map -> Long.valueOf(((Map) map).get("id").toString())))
                .collect(Collectors.toList());
      }

      LOG.debug("Found : {}", items.size());

      for (Object item : items) {
        if (item instanceof Map) {
          Map<String, Object> map = (Map) item;
          for (SearchSelectField field : select.getFields()) {
            if (map.containsKey(field.getName())) {
              map.put(field.getAs(), map.get(field.getName()));
              map.remove(field.getName());
            }
          }

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
      return response.fail("No records provided.");
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

        req.setModel(ScriptBindings.class.getName());
        req.setData(reqData);
        req.setAction(string);

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
    List<Map<String, Object>> actions = new ArrayList<>();

    for (ChartSeries cs : chart.getSeries()) {
      Map<String, Object> map = Maps.newHashMap();
      map.put("key", cs.getKey());
      map.put("type", cs.getType());
      map.put("groupBy", cs.getGroupBy());
      map.put("aggregate", cs.getAggregate());
      map.put("title", cs.getLocalizedTitle());
      map.put("scale", cs.getScale());
      series.add(map);
    }

    if (chart.getConfig() != null) {
      for (ChartConfig c : chart.getConfig()) {
        config.put(c.getName(), c.getValue());
      }
    }

    if (ObjectUtils.notEmpty(chart.getActions())) {
      for (ChartAction chartAction : chart.getActions()) {
        Map<String, Object> chartActionMap = new HashMap<>();
        chartActionMap.put("name", chartAction.getName());
        chartActionMap.put("title", chartAction.getLocalizedTitle());
        chartActionMap.put("action", chartAction.getAction());
        actions.add(chartActionMap);
      }
    }

    data.put("series", series);
    data.put("config", config);
    data.put("actions", actions);
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

      req.setModel(ScriptBindings.class.getName());
      req.setData(request.getData());
      req.setAction(dataSet.getText());

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

  @SuppressWarnings("deprecation")
  private void transformQueryResult(Query query) {
    // TODO: fix deprecation when new transformer api is implemented in hibernate
    query.unwrap(org.hibernate.query.Query.class).setResultTransformer(new DataSetTransformer());
  }

  @SuppressWarnings("serial")
  private static final class DataSetTransformer extends BasicTransformerAdapter {

    @Override
    public Object transformTuple(Object[] tuple, String[] aliases) {
      Map<String, Object> result = new LinkedHashMap<>(tuple.length);
      for (int i = 0; i < tuple.length; ++i) {
        String alias = aliases[i];
        if (alias != null) {
          result.put(alias, tuple[i]);
        }
      }
      return result;
    }
  }
}
