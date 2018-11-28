/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.rpc;

import static com.axelor.common.StringUtils.isBlank;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.db.EntityHelper;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.QueryBinder;
import com.axelor.db.Repository;
import com.axelor.db.ValueEnum;
import com.axelor.db.hibernate.type.JsonFunction;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.axelor.db.search.SearchService;
import com.axelor.i18n.I18n;
import com.axelor.i18n.I18nBundle;
import com.axelor.i18n.L10n;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaPermissions;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.schema.views.Selection;
import com.axelor.rpc.filter.Filter;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.inject.TypeLiteral;
import com.google.inject.persist.Transactional;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;
import javax.validation.ValidationException;
import org.hibernate.StaleObjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class defines CRUD like interface. */
public class Resource<T extends Model> {

  private Class<T> model;

  private Provider<JpaSecurity> security;

  private Logger LOG = LoggerFactory.getLogger(Resource.class);

  private Resource(Class<T> model, Provider<JpaSecurity> security) {
    this.model = model;
    this.security = security;
  }

  @Inject
  @SuppressWarnings("unchecked")
  public Resource(TypeLiteral<T> typeLiteral, Provider<JpaSecurity> security) {
    this((Class<T>) typeLiteral.getRawType(), security);
  }

  /** Returns the resource class. */
  public Class<?> getModel() {
    return model;
  }

  private Long findId(Map<String, Object> values) {
    try {
      return Long.parseLong(values.get("id").toString());
    } catch (Exception e) {
    }
    return null;
  }

  public Response fields() {

    final Response response = new Response();
    final Repository<?> repository = JpaRepository.of(model);

    final Map<String, Object> meta = Maps.newHashMap();
    final List<Object> fields = Lists.newArrayList();

    if (repository == null) {
      for (Property p : JPA.fields(model)) {
        fields.add(p.toMap());
      }
    } else {
      for (Property p : repository.fields()) {
        fields.add(p.toMap());
      }
    }

    meta.put("model", model.getName());
    meta.put("fields", fields);

    response.setData(meta);
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  public static Response models(Request request) {

    Response response = new Response();

    List<String> data = Lists.newArrayList();
    for (Class<?> type : JPA.models()) {
      data.add(type.getName());
    }

    Collections.sort(data);

    response.setData(ImmutableList.copyOf(data));
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  public Response perms() {
    Set<JpaSecurity.AccessType> perms = security.get().getAccessTypes(model, null);
    Response response = new Response();

    response.setData(perms);
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  public Response perms(Long id) {
    Set<JpaSecurity.AccessType> perms = security.get().getAccessTypes(model, id);
    Response response = new Response();

    response.setData(perms);
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  public Response perms(Long id, String perm) {
    Response response = new Response();

    JpaSecurity sec = security.get();
    JpaSecurity.AccessType type = JpaSecurity.CAN_READ;
    try {
      type = JpaSecurity.AccessType.valueOf(perm.toUpperCase());
    } catch (Exception e) {
    }

    try {
      sec.check(type, model, id);
      response.setStatus(Response.STATUS_SUCCESS);
    } catch (Exception e) {
      response.addError(perm, e.getMessage());
      response.setStatus(Response.STATUS_VALIDATION_ERROR);
    }
    return response;
  }

  private List<String> getSortBy(Request request) {

    final List<String> sortBy = Lists.newArrayList();
    final List<String> sortOn = Lists.newArrayList();
    final Mapper mapper = Mapper.of(model);

    boolean unique = false;
    boolean desc = true;

    if (request.getSortBy() != null) {
      sortOn.addAll(request.getSortBy());
    }
    if (sortOn.isEmpty()) {
      Property nameField = mapper.getNameField();
      if (nameField == null) {
        nameField = mapper.getProperty("name");
      }
      if (nameField == null) {
        nameField = mapper.getProperty("code");
      }
      if (nameField != null) {
        sortOn.add(nameField.getName());
      }
    }

    for (String spec : sortOn) {
      String name = spec;
      if (name.startsWith("-")) {
        name = name.substring(1);
      } else {
        desc = false;
      }
      Property property = mapper.getProperty(name);
      if (property == null || property.isPrimary()) {
        // dotted field or primary key
        sortBy.add(spec);
        continue;
      }
      if (property.isReference()) {
        // use name field to sort many-to-one column
        Mapper m = Mapper.of(property.getTarget());
        Property p = m.getNameField();
        if (p != null) {
          spec = spec + "." + p.getName();
        }
      }
      if (property.isUnique() && property.isRequired()) {
        unique = true;
      }
      sortBy.add(spec);
    }

    if (!unique && !(sortBy.contains("id") || sortBy.contains("-id"))) {
      sortBy.add(desc ? "-id" : "id");
    }

    return sortBy;
  }

  private Criteria getCriteria(Request request) {
    if (request.getData() != null) {
      Object domain = request.getData().get("_domain");
      if (domain != null) {
        try {
          String qs = request.getCriteria().createQuery(model).toString();
          JPA.em().createQuery(qs);
        } catch (Exception e) {
          LOG.error("Error: " + e.getMessage(), e);
          throw new IllegalArgumentException("Invalid domain: " + domain, e);
        }
      }
    }
    return request.getCriteria();
  }

  private boolean shouldCheckPermissions(Request request) {
    final Context context = request.getContext();
    // if o2m/m2m search request
    if (context != null
        && context.containsKey("_field")
        && context.containsKey("_field_ids")
        && context.containsKey("id")) {
      final Model parent = context.asType(Model.class);
      return !security
          .get()
          .isPermitted(JpaSecurity.CAN_READ, EntityHelper.getEntityClass(parent), parent.getId());
    }
    return true;
  }

  private Query<?> getQuery(Request request) {
    return getQuery(request, security.get().getFilter(JpaSecurity.CAN_READ, model));
  }

  private Query<?> getQuery(Request request, Filter filter) {
    Criteria criteria = getCriteria(request);
    Query<?> query = JPA.all(model);

    if (criteria != null) {
      query = criteria.createQuery(model, filter);
    } else if (filter != null) {
      query = filter.build(model);
    }

    for (String spec : getSortBy(request)) {
      query = query.order(spec);
    }

    return query;
  }

  private Query<?> getSearchQuery(Request request, Filter filter) {
    final SearchService searchService = Beans.get(SearchService.class);
    if (request.getData() == null || !searchService.isEnabled()) {
      return filter == null ? getQuery(request) : getQuery(request, filter);
    }

    final Map<String, Object> data = request.getData();
    final String searchText = (String) data.get("_searchText");

    // try full-text search
    if (!StringUtils.isEmpty(searchText)) {
      try {
        final List<Long> ids = searchService.fullTextSearch(model, searchText, request.getLimit());
        if (ids.size() > 0) {
          return JPA.all(model).filter("self.id in :ids").bind("ids", ids);
        }
      } catch (Exception e) {
        // just log and fallback to default search
        LOG.error("Unable to do full-text search: " + e.getMessage(), e);
      }
    }
    return filter == null ? getQuery(request) : getQuery(request, filter);
  }

  @SuppressWarnings("all")
  public Response search(Request request) {

    final Filter filter = security.get().getFilter(JpaSecurity.CAN_READ, model);
    boolean check = filter == null || shouldCheckPermissions(request);

    if (check) {
      security.get().check(JpaSecurity.CAN_READ, model);
    }

    LOG.debug("Searching '{}' with {}", model.getCanonicalName(), request.getData());

    Response response = new Response();

    int offset = request.getOffset();
    int limit = request.getLimit();

    Query<?> query = getSearchQuery(request, check ? filter : null).cacheable().readOnly();
    List<?> data = null;
    try {
      if (limit > 0) {
        response.setTotal(query.count());
      }
      if (request.getFields() != null) {
        Query<?>.Selector selector = query.select(request.getFields().toArray(new String[] {}));
        LOG.debug("JPQL: {}", selector);
        data = selector.fetch(limit, offset);
      } else {
        LOG.debug("JPQL: {}", query);
        data = query.fetch(limit, offset);
      }
      if (limit <= 0) {
        response.setTotal(data.size());
      }
    } catch (Exception e) {
      EntityTransaction txn = JPA.em().getTransaction();
      if (txn.isActive()) {
        txn.rollback();
      }
      data = Lists.newArrayList();
      LOG.error("Error: {}", e, e);
    }

    LOG.debug("Records found: {}", data.size());

    final Repository repo = JpaRepository.of(model);
    final List<Object> jsonData = new ArrayList<>();
    final boolean populate =
        request.getContext() != null && request.getContext().get("_populate") != Boolean.FALSE;

    for (Object item : data) {
      if (item instanceof Model) {
        item = toMap(item);
      }
      if (item instanceof Map) {
        Map<String, Object> map = (Map) item;
        if (User.class.isAssignableFrom(model)) {
          map.remove("password");
        }
        if (populate) {
          item = repo.populate(map, request.getContext());
        }
        Translator.applyTranslatables(map, model);
      }
      jsonData.add(item);
    }

    try {
      // check for children (used by tree view)
      doChildCount(request, jsonData);
    } catch (NullPointerException | ClassCastException e) {
    }
    ;

    response.setData(jsonData);
    response.setOffset(offset);
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  @SuppressWarnings("all")
  private void doChildCount(Request request, List<?> result)
      throws NullPointerException, ClassCastException {

    if (result == null || result.isEmpty()) {
      return;
    }

    final Map context = (Map) request.getData().get("_domainContext");
    final Map childOn = (Map) context.get("_childOn");
    final String countOn = (String) context.get("_countOn");

    if (countOn == null && childOn == null) {
      return;
    }

    final StringBuilder builder = new StringBuilder();
    final List ids = Lists.newArrayList();

    for (Object item : result) {
      ids.add(((Map) item).get("id"));
    }

    String modelName = model.getName();
    String parentName = countOn;
    if (childOn != null) {
      modelName = (String) childOn.get("model");
      parentName = (String) childOn.get("parent");
    }

    builder
        .append("SELECT new map(_parent.id as id, count(self.id) as count) FROM ")
        .append(modelName)
        .append(" self ")
        .append("LEFT JOIN self.")
        .append(parentName)
        .append(" AS _parent ")
        .append("WHERE _parent.id IN (:ids) GROUP BY _parent");

    javax.persistence.Query q = JPA.em().createQuery(builder.toString());
    q.setParameter("ids", ids);

    QueryBinder.of(q).setCacheable().setReadOnly();

    Map counts = Maps.newHashMap();
    for (Object item : q.getResultList()) {
      counts.put(((Map) item).get("id"), ((Map) item).get("count"));
    }

    for (Object item : result) {
      ((Map) item).put("_children", counts.get(((Map) item).get("id")));
    }
  }

  private static final int DEFAULT_EXPORT_MAX_SIZE = -1;
  private static final int DEFAULT_EXPORT_FETCH_SIZE = 500;

  private static final int EXPORT_MAX_SIZE =
      AppSettings.get().getInt("data.export.max-size", DEFAULT_EXPORT_MAX_SIZE);
  private static final int EXPORT_FETCH_SIZE =
      AppSettings.get().getInt("data.export.fetch-size", DEFAULT_EXPORT_FETCH_SIZE);

  @SuppressWarnings("all")
  public int export(Request request, Writer writer) throws IOException {
    security.get().check(JpaSecurity.CAN_READ, model);
    security.get().check(JpaSecurity.CAN_EXPORT, model);
    LOG.debug("Exporting '{}' with {}", model.getName(), request.getData());

    List<String> fields = request.getFields();
    List<String> header = new ArrayList<>();
    List<String> names = new ArrayList<>();
    Map<Integer, Map<String, String>> selection = new HashMap<>();
    Map<String, Map<String, Object>> jsonFieldsMap = new HashMap<>();

    Mapper mapper = Mapper.of(model);
    MetaPermissions perms = Beans.get(MetaPermissions.class);

    final Function<String, Map<String, Object>> findJsonFields =
        name ->
            jsonFieldsMap.computeIfAbsent(
                name,
                (n) -> {
                  return MetaJsonRecord.class.isAssignableFrom(model)
                      ? MetaStore.findJsonFields((String) request.getContext().get("jsonModel"))
                      : MetaStore.findJsonFields(model.getName(), n);
                });

    final Function<String, List<String>> findJsonPaths =
        name -> {
          final Map<String, Object> map = findJsonFields.apply(name);
          return map == null
              ? Collections.EMPTY_LIST
              : map.keySet().stream().map(n -> name + "." + n).collect(Collectors.toList());
        };

    if (fields == null) {
      fields = new ArrayList<>();
    }

    if (fields.isEmpty() && MetaJsonRecord.class.isAssignableFrom(model)) {
      fields.add("id");
      fields.addAll(findJsonPaths.apply("attrs"));
    }

    if (fields.isEmpty()) {
      fields.add("id");
      try {
        fields.add(mapper.getNameField().getName());
      } catch (Exception e) {
      }

      for (Property property : mapper.getProperties()) {
        if (property.isPrimary()
            || property.isTransient()
            || property.isVersion()
            || property.isCollection()
            || property.isPassword()
            || property.getType() == PropertyType.BINARY) {
          continue;
        }
        String name = property.getName();
        if (fields.contains(name) || name.matches("^(created|updated)(On|By)$")) {
          continue;
        }
        if (property.isJson()) {
          fields.addAll(findJsonPaths.apply(property.getName()));
        } else {
          fields.add(name);
        }
      }
    }

    for (String field : fields) {
      Iterator<String> iter = Splitter.on(".").split(field).iterator();
      Property prop = mapper.getProperty(iter.next());

      while (iter.hasNext() && prop != null && !prop.isJson()) {
        prop = Mapper.of(prop.getTarget()).getProperty(iter.next());
      }
      if (prop == null
          || prop.isCollection()
          || prop.isTransient()
          || prop.getType() == PropertyType.BINARY) {
        continue;
      }
      if (prop.isJson() && !iter.hasNext()) {
        continue;
      }

      String name = prop.getName();
      String title = prop.getTitle();
      String model = getModel().getName();
      if (prop.isReference()) {
        model = prop.getTarget().getName();
      }
      if (!perms.canExport(AuthUtils.getUser(), model, name)) {
        continue;
      }
      if (iter != null) {
        name = field;
      }

      List<Selection.Option> options = MetaStore.getSelectionList(prop.getSelection());

      if (prop.isJson()) {
        Map<String, Object> jsonFields = findJsonFields.apply(prop.getName());
        Map<String, Object> jsonField = (Map) jsonFields.get(iter.next());
        name = field;
        if (jsonField != null) {
          title = (String) jsonField.get("title");
          if (title == null) {
            title = (String) jsonField.get("autoTitle");
          }
          options = (List) jsonField.get("selectionList");
          if ("many-to-one".equals(jsonField.get("type"))) {
            try {
              String targetName = jsonField.get("targetName").toString();
              targetName = targetName.substring(targetName.indexOf(".") + 1);
              name = name + "." + targetName;
            } catch (Exception e) {
            }
          }
        }
      }
      if (isBlank(title)) {
        title = Inflector.getInstance().humanize(prop.getName());
      }

      if (prop.isReference()) {
        prop = Mapper.of(prop.getTarget()).getNameField();
        if (prop == null) {
          continue;
        }
        name = name + '.' + prop.getName();
      } else if (options != null && !options.isEmpty()) {
        Map<String, String> map = new HashMap<>();
        for (Selection.Option option : options) {
          map.put(option.getValue(), option.getLocalizedTitle());
        }
        selection.put(header.size(), map);
      }

      title = I18n.get(title);

      names.add(name);
      header.add(escapeCsv(title));
    }

    writer.write(Joiner.on(";").join(header));

    int limit =
        EXPORT_MAX_SIZE > 0 ? Math.min(EXPORT_FETCH_SIZE, EXPORT_MAX_SIZE) : EXPORT_FETCH_SIZE;
    int offset = 0;
    int count = 0;

    Query<?> query = getQuery(request);
    Query<?>.Selector selector = query.select(names.toArray(new String[0]));

    List<?> data = selector.values(limit, offset);

    final L10n formatter = L10n.getInstance();

    while (!data.isEmpty()) {
      for (Object item : data) {
        List<?> row = (List<?>) item;
        List<String> line = new ArrayList<>();
        int index = 0;
        for (Object value : row) {
          if (index++ < 2) continue; // ignore first two items (id, version)
          Object objValue = value == null ? "" : value;
          if (selection.containsKey(index - 3)) {
            objValue = selection.get(index - 3).get(objValue.toString());
          }
          if (objValue instanceof Number) {
            objValue = formatter.format((Number) objValue, false);
          }
          if (objValue instanceof LocalDate) {
            objValue = formatter.format((LocalDate) objValue);
          }
          if (objValue instanceof LocalDateTime) {
            objValue = formatter.format((LocalDateTime) objValue);
          }
          if (objValue instanceof ZonedDateTime) {
            objValue = formatter.format((ZonedDateTime) objValue);
          }
          String strValue = objValue == null ? "" : escapeCsv(objValue.toString());
          line.add(strValue);
        }
        writer.write("\n");
        writer.write(Joiner.on(";").join(line));
      }

      count += data.size();

      int nextLimit = limit;
      if (EXPORT_MAX_SIZE > -1) {
        if (count >= EXPORT_MAX_SIZE) {
          break;
        }
        nextLimit = Math.min(limit, EXPORT_MAX_SIZE - count);
      }

      offset += limit;
      data = selector.values(nextLimit, offset);
    }

    return count;
  }

  private String escapeCsv(String value) {
    if (value == null) return "";
    if (value.indexOf('"') > -1) value = value.replaceAll("\"", "\"\"");
    return '"' + value + '"';
  }

  public Response read(long id) {
    security.get().check(JpaSecurity.CAN_READ, model, id);
    Response response = new Response();
    List<Object> data = Lists.newArrayList();

    Model entity = JPA.find(model, id);
    if (entity != null) data.add(entity);
    response.setData(data);
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  public Response fetch(long id, Request request) {
    security.get().check(JpaSecurity.CAN_READ, model, id);

    final Response response = new Response();
    final Repository<?> repository = JpaRepository.of(model);
    final Model entity = repository.find(id);

    if (entity == null) {
      throw new OptimisticLockException(new StaleObjectStateException(model.getName(), id));
    }

    response.setStatus(Response.STATUS_SUCCESS);

    final List<Object> data = Lists.newArrayList();
    final String[] fields =
        request.getFields() == null ? null : request.getFields().toArray(new String[] {});
    final Map<String, Object> values = mergeRelated(request, entity, toMap(entity, fields));

    data.add(repository.populate(values, request.getContext()));
    response.setData(data);
    return response;
  }

  @SuppressWarnings("all")
  private Map<String, Object> mergeRelated(
      Request request, Model entity, Map<String, Object> values) {
    final Map<String, List<String>> related = request.getRelated();
    if (related == null) {
      return values;
    }
    final Mapper mapper = Mapper.of(model);
    related
        .entrySet()
        .stream()
        .filter(e -> e.getValue() != null)
        .filter(e -> e.getValue().size() > 0)
        .forEach(
            e -> {
              final String name = e.getKey();
              final String[] names = e.getValue().toArray(new String[] {});
              Object old = values.get(name);
              Object value = mapper.get(entity, name);
              if (value instanceof Collection<?>) {
                value = Collections2.transform((Collection<?>) value, input -> toMap(input, names));
              } else if (value instanceof Model) {
                value = toMap(value, names);
                if (old instanceof Map) {
                  value = mergeMaps((Map) value, (Map) old);
                }
              }
              values.put(name, value);
            });
    return values;
  }

  @SuppressWarnings("all")
  private Map<String, Object> mergeMaps(Map<String, Object> target, Map<String, Object> source) {
    if (target == null || source == null || source.isEmpty()) {
      return target;
    }
    for (String key : source.keySet()) {
      Object old = source.get(key);
      Object val = target.get(key);
      if (val instanceof Map && old instanceof Map) {
        mergeMaps((Map) val, (Map) old);
      } else if (val == null) {
        target.put(key, old);
      }
    }
    return target;
  }

  public Response verify(Request request) {
    Response response = new Response();
    try {
      JPA.verify(model, request.getData());
      response.setStatus(Response.STATUS_SUCCESS);
    } catch (OptimisticLockException e) {
      response.setStatus(Response.STATUS_VALIDATION_ERROR);
    }
    return response;
  }

  private User changeUserPassword(User user, Map<String, Object> values) {
    final String oldPassword = (String) values.get("oldPassword");
    final String newPassword = (String) values.get("newPassword");
    final String chkPassword = (String) values.get("chkPassword");

    // no password change
    if (StringUtils.isBlank(newPassword)) {
      return user;
    }

    if (StringUtils.isBlank(oldPassword)) {
      throw new ValidationException("Current user password is not provided.");
    }

    if (!newPassword.equals(chkPassword)) {
      throw new ValidationException("Confirm password doesn't match with new password.");
    }

    final User current = AuthUtils.getUser();
    final AuthService authService = AuthService.getInstance();

    if (!authService.match(oldPassword, current.getPassword())) {
      throw new ValidationException("Current user password is wrong.");
    }

    user.setPassword(authService.encrypt(newPassword));

    return user;
  }

  @Transactional
  @SuppressWarnings("all")
  public Response save(final Request request) {

    final Response response = new Response();
    final Repository repository = JpaRepository.of(model);

    List<Object> records = request.getRecords();
    List<Object> data = Lists.newArrayList();

    if ((records == null || records.isEmpty()) && request.getData() == null) {
      response.setStatus(Response.STATUS_FAILURE);
      return response;
    }

    if (records == null) {
      records = Lists.newArrayList();
      records.add(request.getData());
    }

    String[] names = {};
    if (request.getFields() != null) {
      names = request.getFields().toArray(names);
    }

    for (Object record : records) {

      if (record == null) {
        continue;
      }

      record = (Map) repository.validate((Map) record, request.getContext());

      Long id = findId((Map) record);
      JpaSecurity.AccessType accessType =
          id == null || id <= 0L ? JpaSecurity.CAN_CREATE : JpaSecurity.CAN_WRITE;

      if (id == null || id <= 0L) {
        security.get().check(JpaSecurity.CAN_CREATE, model);
      }

      Map<String, Object> orig = (Map) ((Map) record).get("_original");
      JPA.verify(model, orig);

      Model bean = JPA.edit(model, (Map) record);
      id = bean.getId();

      if (bean != null && id != null && id > 0L) {
        security.get().check(JpaSecurity.CAN_WRITE, model, id);
      }

      // if user, update password
      if (bean instanceof User) {
        changeUserPassword((User) bean, (Map) record);
      }

      bean = JPA.manage(bean);
      if (repository != null) {
        bean = repository.save(bean);
      }

      // check permission rules again
      security.get().check(accessType, model, bean.getId());

      // if it's a translation object, invalidate cache
      if (bean instanceof MetaTranslation) {
        I18nBundle.invalidate();
      }

      data.add(repository.populate(toMap(bean, names), request.getContext()));
    }

    response.setData(data);
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  @Transactional
  public Response updateMass(Request request) {

    security.get().check(JpaSecurity.CAN_WRITE, model);

    LOG.debug("Mass update '{}' with {}", model.getCanonicalName(), request.getData());

    Response response = new Response();

    Query<?> query = getQuery(request);
    List<?> data = request.getRecords();

    LOG.debug("JPQL: {}", query);

    @SuppressWarnings("all")
    Map<String, Object> values = (Map) data.get(0);
    response.setTotal(query.update(values, AuthUtils.getUser()));

    LOG.debug("Records updated: {}", response.getTotal());

    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  @Transactional
  @SuppressWarnings("all")
  public Response remove(long id, Request request) {

    security.get().check(JpaSecurity.CAN_REMOVE, model, id);
    final Response response = new Response();
    final Repository repository = JpaRepository.of(model);
    final Map<String, Object> data = Maps.newHashMap();

    data.put("id", id);
    data.put("version", request.getData().get("version"));

    Model bean = JPA.edit(model, data);
    if (bean.getId() != null) {
      if (repository == null) {
        JPA.remove(bean);
      } else {
        repository.remove(bean);
      }
    }

    response.setData(ImmutableList.of(toMapCompact(bean)));
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  @Transactional
  @SuppressWarnings("all")
  public Response remove(Request request) {

    final Response response = new Response();
    final Repository repository = JpaRepository.of(model);
    final List<Object> records = request.getRecords();

    if (records == null || records.isEmpty()) {
      return response.fail("No records provides.");
    }

    final List<Model> entities = Lists.newArrayList();

    for (Object record : records) {
      Map map = (Map) record;
      Long id = Longs.tryParse(map.get("id").toString());
      Integer version = null;
      try {
        version = Ints.tryParse(map.get("version").toString());
      } catch (Exception e) {
      }

      security.get().check(JpaSecurity.CAN_REMOVE, model, id);
      Model bean = JPA.find(model, id);

      if (bean == null || (version != null && !Objects.equal(version, bean.getVersion()))) {
        throw new OptimisticLockException(new StaleObjectStateException(model.getName(), id));
      }
      entities.add(bean);
    }

    for (Model entity : entities) {
      if (JPA.em().contains(entity)) {
        if (repository == null) {
          JPA.remove(entity);
        } else {
          repository.remove(entity);
        }
      }
    }

    response.setData(records);
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  private void fixLinks(Object bean) {
    if (bean == null) return;
    final Mapper mapper = Mapper.of(EntityHelper.getEntityClass(bean));
    for (Property prop : mapper.getProperties()) {
      if (prop.getType() == PropertyType.ONE_TO_MANY && prop.get(bean) != null) {
        for (Object item : (Collection<?>) prop.get(bean)) {
          prop.setAssociation(item, null);
          fixLinks(item);
        }
      }
    }
  }

  @SuppressWarnings("all")
  public Response copy(long id) {
    security.get().check(JpaSecurity.CAN_CREATE, model, id);
    final Response response = new Response();
    final Repository repository = JpaRepository.of(model);

    Model bean = JPA.find(model, id);
    if (repository == null) {
      bean = JPA.copy(bean, true);
    } else {
      bean = repository.copy(bean, true);
    }

    // break bi-directional links
    fixLinks(bean);

    response.setData(ImmutableList.of(bean));
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  public ActionResponse action(ActionRequest request) {

    ActionResponse response = new ActionResponse();
    String[] parts = request.getAction().split("\\:");

    if (parts.length != 2) {
      response.setStatus(Response.STATUS_FAILURE);
      return response;
    }

    String controller = parts[0];
    String method = parts[1];

    try {
      Class<?> klass = Class.forName(controller);
      Method m = klass.getDeclaredMethod(method, ActionRequest.class, ActionResponse.class);
      Object obj = Beans.get(klass);

      m.setAccessible(true);
      m.invoke(obj, new Object[] {request, response});

      response.setStatus(Response.STATUS_SUCCESS);
    } catch (Exception e) {
      LOG.debug(e.toString(), e);
      response.setException(e);
    }
    return response;
  }

  /**
   * Get the name of the record. This method should be used to get the value of name field if it's a
   * function field.
   *
   * @param request the request containing the current values of the record
   * @return response with the updated values with record name
   */
  public Response getRecordName(Request request) {

    Response response = new Response();

    Mapper mapper = Mapper.of(model);
    Map<String, Object> data = request.getData();

    String name = request.getFields().get(0);

    if (name == null) {
      name = "id";
    }

    Property property = null;
    try {
      property = mapper.getProperty(name);
    } catch (Exception e) {
    }

    String selectName = null;

    if (property == null && name.indexOf('.') > -1) {
      JsonFunction func = JsonFunction.fromPath(name);
      Property p = mapper.getProperty(func.getField());
      if (p != null && p.isJson()) {
        selectName = func.toString();
      }
    }

    if (property == null && selectName == null) {
      property = mapper.getNameField();
    }

    if (property != null && selectName == null) {
      selectName = "self." + property.getName();
      name = property.getName();
    }

    if (selectName != null) {
      String qs =
          String.format(
              "SELECT %s FROM %s self WHERE self.id = :id", selectName, model.getSimpleName());

      javax.persistence.Query query = JPA.em().createQuery(qs);
      QueryBinder.of(query).setCacheable().setReadOnly().bind(data);

      Object value = query.getSingleResult();
      data.put(name, value);
    }

    response.setData(ImmutableList.of(data));
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  public static Map<String, Object> toMap(Object bean, String... names) {
    return _toMap(bean, unflatten(null, names), false, 0);
  }

  public static Map<String, Object> toMapCompact(Object bean) {
    return _toMap(bean, null, true, 1);
  }

  @SuppressWarnings("all")
  private static Map<String, Object> _toMap(
      Object bean, Map<String, Object> fields, boolean compact, int level) {

    if (bean == null) {
      return null;
    }

    bean = EntityHelper.getEntity(bean);

    if (fields == null) {
      fields = Maps.newHashMap();
    }

    Map<String, Object> result = new HashMap<String, Object>();
    Mapper mapper = Mapper.of(bean.getClass());

    boolean isSaved = ((Model) bean).getId() != null;
    boolean isCompact = compact || fields.containsKey("$version");

    final Set<Property> translatables = new HashSet<>();

    if ((isCompact && isSaved) || (isSaved && level >= 1) || (level > 1)) {

      Property pn = mapper.getNameField();
      Property pc = mapper.getProperty("code");

      result.put("id", mapper.get(bean, "id"));
      result.put("$version", mapper.get(bean, "version"));

      if (pn != null) {
        result.put(pn.getName(), mapper.get(bean, pn.getName()));
      }
      if (pc != null) {
        result.put(pc.getName(), mapper.get(bean, pc.getName()));
      }

      if (pn != null && pn.isTranslatable()) {
        Translator.translate(result, pn);
      }
      if (pc != null && pc.isTranslatable()) {
        Translator.translate(result, pc);
      }

      for (String name : fields.keySet()) {
        Object child = mapper.get(bean, name);
        if (child instanceof Model) {
          child = _toMap(child, (Map) fields.get(name), true, level + 1);
        }
        if (child != null) {
          result.put(name, child);
        }
      }
      return result;
    }

    for (final Property prop : mapper.getProperties()) {

      String name = prop.getName();
      PropertyType type = prop.getType();

      if (type == PropertyType.BINARY || prop.isPassword()) {
        continue;
      }

      if (isSaved
          && !name.matches("id|version|archived")
          && !fields.isEmpty()
          && !fields.containsKey(name)) {
        continue;
      }

      Object value = mapper.get(bean, name);

      if (name.equals("archived") && value == null) {
        continue;
      }

      if (prop.isImage() && byte[].class.isInstance(value)) {
        value = new String((byte[]) value);
      }

      // decimal values should be rounded accordingly otherwise the
      // json mapper may use wrong scale.
      if (value instanceof BigDecimal) {
        BigDecimal decimal = (BigDecimal) value;
        int scale = prop.getScale();
        if (decimal.scale() == 0 && scale > 0 && scale != decimal.scale()) {
          value = decimal.setScale(scale, RoundingMode.HALF_UP);
        }
      }

      if (value instanceof Model) { // m2o
        Map<String, Object> _fields = (Map) fields.get(prop.getName());
        value = _toMap(value, _fields, true, level + 1);
      }

      if (value instanceof Collection) { // o2m | m2m
        List<Object> items = Lists.newArrayList();
        for (Model input : (Collection<Model>) value) {
          Map<String, Object> item;
          if (input.getId() != null) {
            item = _toMap(input, null, true, level + 1);
          } else {
            item = _toMap(input, null, false, 1);
          }
          if (item != null) {
            items.add(item);
          }
        }
        value = items;
      }

      result.put(name, value);

      if (prop.isTranslatable() && value instanceof String) {
        Translator.translate(result, prop);
      }

      // include custom enum value
      if (prop.isEnum() && value instanceof ValueEnum<?>) {
        String enumName = ((Enum<?>) value).name();
        Object enumValue = ((ValueEnum<?>) value).getValue();
        if (!Objects.equal(enumName, enumValue)) {
          result.put(name + "$value", ((ValueEnum<?>) value).getValue());
        }
      }

      // special case for User/Group objects
      if (result.get("homeAction") != null) {
        MetaAction act =
            JpaRepository.of(MetaAction.class)
                .all()
                .filter("self.name = ?", result.get("homeAction"))
                .fetchOne();
        if (act != null) {
          result.put("__actionSelect", toMapCompact(act));
        }
      }
    }

    return result;
  }

  @SuppressWarnings("all")
  private static Map<String, Object> unflatten(Map<String, Object> map, String... names) {
    if (map == null) map = Maps.newHashMap();
    if (names == null) return map;
    for (String name : names) {
      if (map.containsKey(name)) continue;
      if (name.contains(".")) {
        String[] parts = name.split("\\.", 2);
        Map<String, Object> child = (Map) map.get(parts[0]);
        if (child == null) {
          child = Maps.newHashMap();
        }
        map.put(parts[0], unflatten(child, parts[1]));
      } else {
        map.put(name, Maps.newHashMap());
      }
    }
    return map;
  }
}
