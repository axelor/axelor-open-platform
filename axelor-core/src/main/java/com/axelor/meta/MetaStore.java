/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta;

import static com.axelor.common.StringUtils.isBlank;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.cache.AxelorCache;
import com.axelor.cache.CacheBuilder;
import com.axelor.common.Inflector;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.JpaSecurity;
import com.axelor.db.JpaSecurity.AccessType;
import com.axelor.db.Model;
import com.axelor.db.Nulls;
import com.axelor.db.Query;
import com.axelor.db.ValueEnum;
import com.axelor.db.annotations.EnumWidget;
import com.axelor.db.json.JsonReferenceFieldDTO;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonModel;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.MetaPermissionRule;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.repo.MetaJsonModelRepository;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.views.Selection;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MetaStore {

  private static final Logger log = LoggerFactory.getLogger(MetaStore.class);

  private static final AxelorCache<String, Action> ACTIONS =
      CacheBuilder.newBuilder("actions").maximumSize(1000).build(XMLViews::findAction);

  private static final AxelorCache<ModelFieldKey, Map<String, JsonReferenceFieldDTO>> JSON_FIELDS =
      CacheBuilder.newBuilder("jsonFields")
          .maximumSize(1000)
          .expireAfterAccess(Duration.ofDays(1))
          .build();

  private static class NullMap<K, V> extends HashMap<K, V> {}

  private static final Map<String, JsonReferenceFieldDTO> NULL_JSON_FIELD = new NullMap<>();

  /** Reference (relational) custom fields keyed by their owning model or custom model. */
  private static final AxelorCache<String, List<JsonReferenceFieldDTO>> REFERENCE_JSON_FIELDS =
      CacheBuilder.newBuilder("referenceJsonFieldCache")
          .expireAfterWrite(Duration.ofHours(1))
          .build(MetaStore::loadReferenceJsonFields);

  /** Reverse index: custom fields keyed by the model they reference (their target). */
  private static final AxelorCache<String, List<JsonReferenceFieldDTO>> TARGET_JSON_FIELDS =
      CacheBuilder.newBuilder("targetJsonFieldCache")
          .expireAfterWrite(Duration.ofHours(1))
          .build(MetaStore::loadTargetJsonFields);

  /** Key used to store and retrieve JSON fields from the json fields cache. */
  private record ModelFieldKey(String modelName, String modelField) implements Serializable {
    public static ModelFieldKey of(String modelName, String modelField) {
      return new ModelFieldKey(modelName, modelField);
    }

    public static ModelFieldKey of(String jsonModel) {
      return new ModelFieldKey(jsonModel, null);
    }
  }

  private MetaStore() {}

  /** Used for unit testing. */
  static void register(ObjectViews views) {
    try {
      for (Action item : views.getActions()) {
        ACTIONS.put(item.getName(), item);
      }
    } catch (NullPointerException e) {
    }
  }

  public static Action getAction(String name) {
    Action action = ACTIONS.get(name);
    if (action == null) {
      return null;
    }
    final String module = action.getModuleToCheck();
    if (StringUtils.isBlank(module) || ModuleManager.isInstalled(module)) {
      return action;
    }
    return null;
  }

  public static Map<String, Object> getPermissions(Property property) {
    final Map<String, Object> map = new HashMap<>();
    MetaPermissions perms = Beans.get(MetaPermissions.class);
    final User user = AuthUtils.getUser();
    if (user == null || AuthUtils.isAdmin(user)) {
      return null;
    }

    // Field permissions
    map.put("read", perms.canRead(user, property.getEntity().getName(), property.getName()));
    map.put("write", perms.canWrite(user, property.getEntity().getName(), property.getName()));
    map.put("export", perms.canExport(user, property.getEntity().getName(), property.getName()));

    // Model permissions
    final Map<String, Object> modelPerms =
        property.getTarget() != null ? getPermissions(property.getTarget()) : new HashMap<>();

    if (ObjectUtils.isEmpty(modelPerms)) {
      return map;
    }

    // Merge Model & Field permissions
    for (String key : modelPerms.keySet()) {
      boolean modelPerm =
          ObjectUtils.isEmpty(modelPerms.get(key))
              || Boolean.parseBoolean(modelPerms.get(key).toString());
      if (map.containsKey(key)) {
        map.replace(key, Boolean.parseBoolean(map.get(key).toString()) && modelPerm);
      } else {
        map.put(key, modelPerm);
      }
    }

    return map;
  }

  public static Map<String, Object> getPermissions(Class<?> model) {
    final User user = AuthUtils.getUser();
    if (user == null || AuthUtils.isAdmin(user) || !Model.class.isAssignableFrom(model)) {
      return null;
    }

    @SuppressWarnings("unchecked")
    final Class<? extends Model> klass = (Class<? extends Model>) model;
    final Map<String, Object> map = new HashMap<>();
    final JpaSecurity security = Beans.get(JpaSecurity.class);

    map.put("read", security.isPermitted(AccessType.READ, klass));
    map.put("write", security.isPermitted(AccessType.WRITE, klass));
    map.put("create", security.isPermitted(AccessType.CREATE, klass));
    map.put("remove", security.isPermitted(AccessType.REMOVE, klass));
    map.put("export", security.isPermitted(AccessType.EXPORT, klass));

    return map;
  }

  private static Property findField(final Mapper mapper, String name) {
    final Iterator<String> iter = Splitter.on(".").split(name).iterator();
    Mapper current = mapper;
    Property property = current.getProperty(iter.next());

    if (property == null || (property.isJson() && iter.hasNext())) {
      return null;
    }

    while (property != null && property.getTarget() != null && iter.hasNext()) {
      current = Mapper.of(property.getTarget());
      property = current.getProperty(iter.next());
    }

    return property;
  }

  public static Map<String, Object> findFields(
      final Class<?> modelClass, final Collection<String> names) {
    return findFields(modelClass, names, null);
  }

  public static Map<String, Object> findFields(
      final Class<?> modelClass, final Collection<String> names, String jsonModel) {
    final Map<String, Object> data = new HashMap<>();
    final Mapper mapper = Mapper.of(modelClass);
    final Map<String, Property> fieldsMap = new LinkedHashMap<>();
    final List<Object> fields = new ArrayList<>();

    Object bean = null;
    try {
      bean = modelClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
    }

    for (final String name : names) {
      final Property property = findField(mapper, name);
      if (property == null) continue;
      final Map<String, Object> map = property.toMap();
      map.put("name", name);
      if (property.getSelection() != null && !"".equals(property.getSelection().trim())) {
        map.put("selection", property.getSelection());
        map.put("selectionList", getSelectionList(property.getSelection()));
      }
      if (property.isEnum()) {
        map.put("selectionList", getSelectionList(property.getEnumType()));
      }
      map.put("perms", getPermissions(property));
      // find the default value
      if (!property.isTransient() && !property.isVirtual()) {
        Object obj = null;
        if (name.contains(".")) {
          try {
            obj = property.getEntity().getDeclaredConstructor().newInstance();
          } catch (Exception e) {
          }
        } else {
          obj = bean;
        }
        if (obj != null) {
          Object defaultValue = property.get(obj);
          if (defaultValue != null) {
            map.put("defaultValue", defaultValue);
          }
        }
      }
      if (name.contains(".")) {
        map.put("readonly", true);
      }
      fieldsMap.put(name, property);
      fields.add(map);
    }

    Map<String, Object> perms = getPermissions(modelClass);

    data.put("perms", perms);
    data.put("fields", fields);

    // Don't process dotted json fields for custom models if jsonModel is not given
    if (MetaJsonRecord.class.isAssignableFrom(modelClass) && StringUtils.isBlank(jsonModel)) {
      return data;
    }

    // find dotted json fields
    final Map<String, Map<String, Object>> jsonFields = new HashMap<>();
    for (String name : names) {
      if (fieldsMap.containsKey(name) || name.indexOf('.') == -1) {
        continue;
      }
      final String first = name.substring(0, name.indexOf('.'));
      final String field = name.substring(name.indexOf('.') + 1);
      final Property property = findField(mapper, first);
      if (property == null || !property.isJson()) {
        continue;
      }
      if (!jsonFields.containsKey(first)) {
        var jsonAttrs =
            StringUtils.isBlank(jsonModel)
                ? findJsonFields(modelClass.getName(), first)
                : findJsonFields(jsonModel);
        jsonFields.put(first, jsonAttrs);
      }
      final Map<String, Object> jsonField = jsonFields.get(first);
      if (jsonField != null && jsonField.containsKey(field)) {
        @SuppressWarnings("all")
        final Map<String, Object> attrs = new HashMap<>((Map) jsonField.get(field));
        if (attrs != null) {
          attrs.put("name", name);
          fields.add(attrs);
        }
      }
    }

    return data;
  }

  private static Map<String, Object> checkPermissions(
      Map<String, Object> fields, String object, String jsonField) {
    final User user = AuthUtils.getUser();
    final MetaPermissions perms = Beans.get(MetaPermissions.class);
    final Map<String, Object> result = new LinkedHashMap<>();

    for (Map.Entry<String, Object> item : fields.entrySet()) {
      String name = jsonField == null ? item.getKey() : jsonField + "." + item.getKey();
      MetaPermissionRule rule = perms.findRule(user, object, name);
      @SuppressWarnings("unchecked")
      Map<String, Object> attrs = (Map<String, Object>) item.getValue();

      if (rule == null) {
        result.put(item.getKey(), attrs);
        continue;
      }

      if (!Boolean.TRUE.equals(rule.getCanRead())) {
        continue;
      }

      if (!attrs.containsKey("readonlyIf") && rule.getReadonlyIf() != null) {
        attrs.put("readonlyIf", rule.getReadonlyIf());
      }

      if (!attrs.containsKey("hideIf") && rule.getHideIf() != null) {
        attrs.put("hideIf", rule.getHideIf());
      }

      if (!Boolean.TRUE.equals(rule.getCanWrite())) {
        attrs.put("readonly", true);
        if (isBlank(rule.getReadonlyIf())) {
          attrs.remove("readonlyIf");
        }
      }

      result.put(item.getKey(), attrs);
    }

    return result;
  }

  @Nullable
  public static Map<String, Object> findJsonFields(String modelName, String modelField) {
    final Map<String, JsonReferenceFieldDTO> raw = getJsonFields(modelName, modelField);
    return raw != null ? resolveJsonFields(raw.values(), modelName, modelField) : null;
  }

  @Nullable
  public static Map<String, Object> findJsonFields(String jsonModel) {
    final Map<String, JsonReferenceFieldDTO> raw = getJsonFields(jsonModel);
    return raw != null ? resolveJsonFields(raw.values(), jsonModel, null) : null;
  }

  /** Finds the JSON field metadata on model. */
  @Nullable
  public static JsonReferenceFieldDTO findJsonField(
      String modelName, String modelField, String fieldName) {
    final Map<String, JsonReferenceFieldDTO> raw = getJsonFields(modelName, modelField);
    return raw != null ? raw.get(fieldName) : null;
  }

  /** Finds the JSON field metadata on json model. */
  @Nullable
  public static JsonReferenceFieldDTO findJsonField(String jsonModel, String fieldName) {
    final Map<String, JsonReferenceFieldDTO> raw = getJsonFields(jsonModel);
    return raw != null ? raw.get(fieldName) : null;
  }

  /** Checks if the JSON field exists on model. */
  public static boolean hasJsonField(String modelName, String modelField, String fieldName) {
    final Map<String, JsonReferenceFieldDTO> raw = getJsonFields(modelName, modelField);
    return raw != null && raw.containsKey(fieldName);
  }

  /** Checks if the JSON field exists on json model. */
  public static boolean hasJsonField(String jsonModel, String fieldName) {
    final Map<String, JsonReferenceFieldDTO> raw = getJsonFields(jsonModel);
    return raw != null && raw.containsKey(fieldName);
  }

  @Nullable
  public static Map<String, JsonReferenceFieldDTO> getJsonFields(
      String modelName, String modelField) {
    return getJsonFieldsOrNull(
        ModelFieldKey.of(modelName, modelField), MetaStore::loadJsonFieldsByModelField);
  }

  @Nullable
  public static Map<String, JsonReferenceFieldDTO> getJsonFields(String jsonModel) {
    if (StringUtils.isBlank(jsonModel)) {
      return null;
    }
    return getJsonFieldsOrNull(ModelFieldKey.of(jsonModel), MetaStore::loadJsonFieldsByJsonModel);
  }

  /**
   * Gets from json fields cache, converting cached null map to null.
   *
   * <p>This prevents repeated cache misses when the loader returns actual null.
   */
  @Nullable
  private static Map<String, JsonReferenceFieldDTO> getJsonFieldsOrNull(
      ModelFieldKey key, Function<ModelFieldKey, Map<String, JsonReferenceFieldDTO>> loader) {
    var jsonFields = JSON_FIELDS.get(key, loader);
    return jsonFields instanceof NullMap ? null : jsonFields;
  }

  /**
   * Returns the relational (reference) custom fields owned by the given model. The key is a
   * fully-qualified class name for real models, or a custom model name for {@link MetaJsonModel}s.
   */
  public static List<JsonReferenceFieldDTO> getReferenceJsonFields(String modelKey) {
    return REFERENCE_JSON_FIELDS.get(modelKey);
  }

  /**
   * Returns the custom fields whose target (relational reference) is the given model. The key is a
   * fully-qualified class name for real models, or a custom model name for {@link MetaJsonModel}s.
   */
  public static List<JsonReferenceFieldDTO> getTargetJsonFields(String targetModel) {
    return TARGET_JSON_FIELDS.get(targetModel);
  }

  private static Map<String, JsonReferenceFieldDTO> loadJsonFieldsByModelField(ModelFieldKey key) {
    String modelName = key.modelName();
    String modelField = key.modelField();

    try {
      Property property = Mapper.of(Class.forName(modelName)).getProperty(modelField);
      if (property == null || !property.isJson()) {
        return NULL_JSON_FIELD;
      }
    } catch (Exception e) {
      return NULL_JSON_FIELD;
    }

    return Query.of(MetaJsonField.class)
        .filter("self.model = :model AND self.modelField = :modelField")
        .bind("model", modelName)
        .bind("modelField", modelField)
        .order("sequence", Nulls.FIRST)
        .order("id")
        .cacheable()
        .fetch()
        .stream()
        .map(JsonReferenceFieldDTO::from)
        .collect(
            Collectors.toMap(
                JsonReferenceFieldDTO::name,
                Function.identity(),
                (existing, replacement) -> existing,
                LinkedHashMap::new));
  }

  private static Map<String, JsonReferenceFieldDTO> loadJsonFieldsByJsonModel(ModelFieldKey key) {
    final MetaJsonModelRepository forms = Beans.get(MetaJsonModelRepository.class);
    final MetaJsonModel found = forms.findByName(key.modelName());

    if (found == null) {
      return NULL_JSON_FIELD;
    }

    if (ObjectUtils.isEmpty(found.getFields())) {
      return Collections.emptyMap();
    }

    return found.getFields().stream()
        .map(JsonReferenceFieldDTO::from)
        .collect(
            Collectors.toMap(
                JsonReferenceFieldDTO::name,
                Function.identity(),
                (existing, replacement) -> existing,
                LinkedHashMap::new));
  }

  private static List<JsonReferenceFieldDTO> loadTargetJsonFields(String targetModel) {
    final String filter =
        targetModel.contains(".")
            ? "self.type = 'many-to-one' AND self.targetModel = :model"
            : "self.type = 'json-many-to-one' AND self.targetJsonModel.name = :model";
    return Query.of(MetaJsonField.class)
        .filter(filter)
        .bind("model", targetModel)
        .cacheable()
        .fetch()
        .stream()
        .map(JsonReferenceFieldDTO::from)
        .toList();
  }

  private static List<JsonReferenceFieldDTO> loadReferenceJsonFields(String modelKey) {
    if (modelKey.contains(".")) {
      final Class<?> modelClass;
      try {
        modelClass = Class.forName(modelKey);
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException("Class not found: " + modelKey, e);
      }
      return Stream.of(Mapper.of(modelClass).getProperties())
          .filter(Property::isJson)
          .map(Property::getName)
          .map(fieldName -> getJsonFields(modelKey, fieldName))
          .filter(Objects::nonNull)
          .flatMap(map -> map.values().stream())
          .filter(JsonReferenceFieldDTO::isReference)
          .toList();
    }

    var jsonFields = getJsonFields(modelKey);

    return jsonFields != null
        ? jsonFields.values().stream().filter(JsonReferenceFieldDTO::isReference).toList()
        : Collections.emptyList();
  }

  /** Builds the field metadata and overlays user locale/access contexts. */
  private static Map<String, Object> resolveJsonFields(
      Collection<JsonReferenceFieldDTO> records, String object, String jsonField) {

    if (ObjectUtils.isEmpty(records)) {
      return Collections.emptyMap();
    }

    final User user = AuthUtils.getUser();
    final boolean roleCheckEnabled = user != null && !AuthUtils.isAdmin(user);
    final Set<Long> userRoleIds = roleCheckEnabled ? collectUserRoleIds(user) : null;
    final ResourceBundle bundle = I18n.getBundle();

    ScriptHelper scriptHelper = null;
    final Map<String, Object> fields = new LinkedHashMap<>();
    final MetaJsonModelRepository forms = Beans.get(MetaJsonModelRepository.class);

    for (JsonReferenceFieldDTO record : records) {
      final Map<String, Object> attrs = new HashMap<>();

      attrs.putAll(record.toMap());

      String type = record.type() == null ? "" : record.type();
      Integer min = record.minSize();
      Integer max = record.maxSize();
      if (min != null && max != null) {
        if (max <= min) {
          attrs.remove("maxSize");
        }
        if (max == 0 && min == 0) {
          attrs.remove("maxSize");
          attrs.remove("minSize");
        }
      }
      if (type.matches("date|time|datetime|boolean")) {
        attrs.remove("maxSize");
        attrs.remove("minSize");
      }

      if ("ref-select".equalsIgnoreCase(record.type())
          || "ref-select".equalsIgnoreCase(record.widget())
          || "RefSelect".equalsIgnoreCase(record.widget())) {
        attrs.put("widget", "json-ref-select");
      }

      if (!StringUtils.isBlank(record.targetModel())) {
        attrs.put("target", record.targetModel());
        attrs.remove("targetModel");
        try {
          Property nameField = Mapper.of(Class.forName(record.targetModel())).getNameField();
          if (nameField != null) {
            attrs.put("targetName", nameField.getName());
          }
        } catch (ClassNotFoundException e) {
          log.warn("Target model not found: {}", record.targetModel());
        }
      }

      if (type.startsWith("json-")) {
        type = type.substring(5);
        attrs.put("type", type);
        attrs.put("target", MetaJsonRecord.class.getName());
        if (record.targetJsonModel() != null) {
          final MetaJsonModel targetModel = forms.findByName(record.targetJsonModel());
          if (targetModel == null) {
            log.warn("Target json model not found: {}", record.targetJsonModel());
            continue;
          }
          String domain = "self.jsonModel = '%s'".formatted(targetModel.getName());
          if (!StringUtils.isBlank(record.domain())) {
            domain = "(%s) AND (%s)".formatted(domain, record.domain());
          }
          attrs.put("domain", domain);
          if (targetModel.getGridView() != null) {
            attrs.put("gridView", targetModel.getGridView().getName());
          }
          if (targetModel.getFormView() != null) {
            attrs.put("formView", targetModel.getFormView().getName());
          }
          attrs.put("targetName", "name");
          attrs.put("jsonTarget", targetModel.getName());
        }
      }

      if (StringUtils.notBlank(record.selection())) {
        attrs.put("selectionList", getSelectionList(record.selection()));
      }

      if (StringUtils.notBlank(record.enumType())) {
        try {
          attrs.put("selectionList", getSelectionList(Class.forName(record.enumType())));
        } catch (ClassNotFoundException e) {
          log.error("No such enum type found: {}", record.enumType());
        }
      }

      attrs.put("jsonField", record.modelField());
      attrs.put("jsonPath", record.name());
      if (type.matches("integer|decimal|boolean")) {
        attrs.put("jsonType", type);
      }

      // localized title (per-locale, applied on read)
      String rawTitle = record.title();
      if (StringUtils.notBlank(rawTitle)) {
        attrs.put("title", bundle.getString(rawTitle));
      } else {
        String last = record.name().substring(record.name().lastIndexOf('.') + 1);
        attrs.put("autoTitle", bundle.getString(Inflector.getInstance().humanize(last)));
      }

      boolean hasAccess = true;
      Set<Long> roleIds = record.roleIds();

      if (roleIds != null) {
        // role check
        if (userRoleIds != null
            && !roleIds.isEmpty()
            && Collections.disjoint(userRoleIds, roleIds)) {
          hasAccess = false;
        }
        attrs.remove("roleIds");
      }

      String includeIf = record.includeIf();

      // server condition
      if (hasAccess && StringUtils.notBlank(includeIf)) {
        if (scriptHelper == null) {
          scriptHelper = new CompositeScriptHelper(null);
        }
        if (!scriptHelper.test(includeIf)) {
          hasAccess = false;
        }
      }

      if (!hasAccess) {
        attrs.put("hidden", true);
        attrs.put("hideIf", "true");
        attrs.remove("showIf");
        attrs.put("forceHidden", true);
      }

      fields.put(record.name(), attrs);
    }
    return checkPermissions(fields, object, jsonField);
  }

  private static Set<Long> collectUserRoleIds(User user) {
    final Set<Long> ids = new HashSet<>();
    if (ObjectUtils.notEmpty(user.getRoles())) {
      user.getRoles().forEach(role -> ids.add(role.getId()));
    }
    if (user.getGroup() != null && ObjectUtils.notEmpty(user.getGroup().getRoles())) {
      user.getGroup().getRoles().forEach(role -> ids.add(role.getId()));
    }
    return ids;
  }

  public static List<Selection.Option> getSelectionList(Class<?> enumType) {
    if (enumType == null || !enumType.isEnum()) {
      return null;
    }
    final List<Selection.Option> all = new ArrayList<>();
    for (Enum<?> item : enumType.asSubclass(Enum.class).getEnumConstants()) {
      final Selection.Option option = new Selection.Option();
      final String name = item.name();

      option.setValue(name);
      Map<String, Object> data = new HashMap<>();
      option.setData(data);

      if (item instanceof ValueEnum<?> enumValue) {
        Object value = enumValue.getValue();
        if (!Objects.equals(name, value)) {
          data.put("value", value);
        }
      }

      try {
        final Field field = enumType.getDeclaredField(name);
        final EnumWidget widget = field.getAnnotation(EnumWidget.class);

        if (widget.hidden()) {
          continue;
        }

        if (StringUtils.notBlank(widget.title())) {
          option.setTitle(widget.title());
        }
        if (StringUtils.notBlank(widget.description())) {
          data.put("description", widget.description());
        }
        if (StringUtils.notBlank(widget.icon())) {
          option.setIcon(widget.icon());
        }
        option.setOrder(widget.order());
      } catch (Exception e) {
      }

      if (option.getTitle() == null) {
        option.setTitle(Inflector.getInstance().humanize(name));
      }

      all.add(option);
    }

    if (all.isEmpty()) {
      return null;
    }

    return sortSelectionOptions(all);
  }

  private static List<Selection.Option> sortSelectionOptions(List<Selection.Option> all) {
    all.sort(
        (o1, o2) -> {
          Integer n = o1.getOrder();
          Integer m = o2.getOrder();

          if (n == null) n = 0;
          if (m == null) m = 0;

          return Integer.compare(n, m);
        });

    return all;
  }

  public static List<Selection.Option> getSelectionList(
      Class<? extends Model> model, String orderBy, int limit) {
    Mapper mapper = Mapper.of(model);
    Property nameField = mapper.getNameField();
    String name = nameField == null ? "id" : nameField.getName();

    Query<?> query = Query.of(model);
    query.filter("self.archived is null OR self.archived = false");

    if (StringUtils.notBlank(orderBy)) {
      query.order(orderBy);
    }

    return query.select(name).fetch(limit, 0).stream()
        .map(record -> (Map<?, ?>) record)
        .map(
            record -> {
              Selection.Option option = new Selection.Option();
              option.setValue(record.get("id").toString());
              Optional.ofNullable(record.get(name))
                  .map(Object::toString)
                  .ifPresent(option::setTitle);
              if (nameField != null && nameField.isTranslatable()) {
                String key = "value:" + option.getTitle();
                String value = I18n.get(key);
                if (!value.equals(key)) {
                  Map<String, Object> data = new HashMap<>();
                  data.put(name, option.getTitle());
                  option.setData(data);
                  option.setTitle(value);
                }
              }
              return option;
            })
        .collect(Collectors.toList());
  }

  public static List<Selection.Option> getSelectionList(String selection) {
    if (StringUtils.isBlank(selection)) {
      return null;
    }

    final Map<String, Selection.Option> all = buildSelectionMap(selection);
    if (all == null) {
      return null;
    }

    return sortSelectionOptions(new ArrayList<>(all.values()));
  }

  public static Selection.Option getSelectionItem(String selection, String value) {
    if (StringUtils.isBlank(selection)) {
      return null;
    }

    final Map<String, Selection.Option> all = buildSelectionMap(selection);
    if (all == null) {
      return null;
    }

    return all.get(value);
  }

  private static Map<String, Selection.Option> buildSelectionMap(String selection) {
    final List<MetaSelectItem> items =
        Query.of(MetaSelectItem.class)
            .filter("self.select.name = ?", selection)
            .order("select.priority")
            .order("order")
            .fetch();

    if (items.isEmpty()) {
      return null;
    }

    final Map<String, Selection.Option> all = new LinkedHashMap<>();

    for (MetaSelectItem item : items) {
      if (item.getHidden().equals(Boolean.TRUE)) {
        all.remove(item.getValue());
      } else {
        all.put(item.getValue(), getSelectionItem(item));
      }
    }

    return all;
  }

  private static Selection.Option getSelectionItem(MetaSelectItem item) {
    final ObjectMapper objectMapper = Beans.get(ObjectMapper.class);
    final Selection.Option option = new Selection.Option();
    option.setValue(item.getValue());
    option.setTitle(item.getTitle());
    option.setIcon(item.getIcon());
    option.setColor(item.getColor());
    option.setOrder(item.getOrder());
    option.setHidden(item.getHidden());
    try {
      option.setData(
          objectMapper.readValue(item.getData(), new TypeReference<Map<String, Object>>() {}));
    } catch (Exception e) {
      // this should never happen, ignore
    }
    return option;
  }

  public static void clear() {
    ACTIONS.invalidateAll();
  }

  public static void invalidate(String name) {
    ACTIONS.invalidate(name);
  }

  public static void invalidateJsonFields() {
    JSON_FIELDS.invalidateAll();
    REFERENCE_JSON_FIELDS.invalidateAll();
    TARGET_JSON_FIELDS.invalidateAll();
  }

  /** Invalidates every cached view affected by a change to the given custom field. */
  public static void invalidateJsonFields(MetaJsonField field) {
    if (field == null) {
      return;
    }
    MetaJsonModel jsonModel = field.getJsonModel();
    final String owner = jsonModel != null ? jsonModel.getName() : field.getModel();
    if (jsonModel != null) {
      invalidateOwnerFields(ModelFieldKey.of(jsonModel.getName()));
    } else {
      invalidateOwnerFields(ModelFieldKey.of(field.getModel(), field.getModelField()));
    }
    invalidateReferenceFields(owner);
    invalidateTargetFields(
        Optional.ofNullable(field.getTargetJsonModel())
            .map(MetaJsonModel::getName)
            .orElse(field.getTargetModel()));
  }

  /** Invalidates every cached view affected by a change to the given custom model. */
  public static void invalidateJsonFields(MetaJsonModel model) {
    if (model == null) {
      return;
    }
    invalidateOwnerFields(ModelFieldKey.of(model.getName()));
    invalidateReferenceFields(model.getName());
    invalidateTargetFields(model.getName());
  }

  private static void invalidateOwnerFields(ModelFieldKey key) {
    if (key.modelName() != null) {
      JSON_FIELDS.invalidate(key);
    }
  }

  private static void invalidateReferenceFields(String modelKey) {
    if (modelKey != null) {
      REFERENCE_JSON_FIELDS.invalidate(modelKey);
    }
  }

  private static void invalidateTargetFields(String targetModel) {
    if (targetModel != null) {
      TARGET_JSON_FIELDS.invalidate(targetModel);
    }
  }
}
