/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.json;

import com.axelor.db.EntityHelper;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.axelor.db.json.JsonReferenceResolver.SourceContext;
import com.axelor.db.mapper.Mapper;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonModel;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.db.Query;
import com.axelor.rpc.JsonContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class JsonReferenceCascader {

  // ThreadLocal holds per-save snapshots keyed by "Model#id#jsonField";
  // always cleared in finally via clearSaveState.
  private static final ThreadLocal<Map<String, Map<String, Object>>> OLD_JSON =
      ThreadLocal.withInitial(HashMap::new);
  // Cascade-visit tracking: prevents infinite loops with circular references (like JPA cascade).
  private static final ThreadLocal<Set<String>> IN_PROGRESS = ThreadLocal.withInitial(HashSet::new);

  @Inject private JsonReferenceResolver resolver;

  public void beforeSave(Model entity) {
    if (!hasJsonField(entity)) return;
    var ctx = SourceContext.of(entity);
    if (ctx.id() == null || ctx.id() <= 0) return;

    var fields = resolver.findReferenceFields(ctx);
    if (fields.isEmpty()) return;

    var grouped = groupByModelField(fields);
    for (var entry : grouped.entrySet()) {
      var jsonField = entry.getKey();
      var snapshotKey = snapshotKey(ctx, jsonField);

      // Skip if snapshot already captured (e.g. from Resource.save before flush)
      if (OLD_JSON.get().containsKey(snapshotKey)) continue;

      var jsonData = resolver.loadJsonFromDb(entity, jsonField);
      if (jsonData == null) continue;

      OLD_JSON.get().put(snapshotKey, jsonData);
    }
  }

  public void afterSave(Model entity) {
    if (!hasJsonField(entity)) return;

    var ctx = SourceContext.of(entity);
    var fields = resolver.findReferenceFields(ctx);
    if (fields.isEmpty()) return;

    var grouped = groupByModelField(fields);
    for (var entry : grouped.entrySet()) {
      var jsonField = entry.getKey();
      var fieldGroup = entry.getValue();
      var snapshotKey = snapshotKey(ctx, jsonField);

      var jsonData = resolver.getJsonValue(entity, jsonField);
      if (jsonData == null) jsonData = Map.of();

      var mutableData = new HashMap<>(jsonData);
      var map = OLD_JSON.get();
      var oldData = map.remove(snapshotKey);
      if (map.isEmpty()) {
        OLD_JSON.remove();
      }

      handleSave(entity, jsonField, fieldGroup, mutableData, oldData);

      if (!mutableData.equals(jsonData)) {
        resolver.setJsonValue(entity, jsonField, mutableData);
      }
    }
  }

  public void clearSaveState(Model entity) {
    if (!hasJsonField(entity)) return;
    var ctx = SourceContext.of(entity);
    if (ctx.id() == null || ctx.id() <= 0) return;

    var map = OLD_JSON.get();
    var prefix = entityKey(ctx) + "#";
    map.keySet().removeIf(key -> key.startsWith(prefix));
    if (map.isEmpty()) {
      OLD_JSON.remove();
    }
  }

  public static void clearCache(String modelKey) {
    JsonReferenceResolver.clearCache(modelKey);
  }

  public void beforeRemove(Model entity) {
    if (entity == null || !hasJsonField(entity)) return;

    var ctx = SourceContext.of(entity);
    if (!enter(ctx)) return;
    try {
      var fields = resolver.findReferenceFields(ctx);
      if (fields.isEmpty()) return;

      var grouped = groupByModelField(fields);
      for (var entry : grouped.entrySet()) {
        var jsonField = entry.getKey();
        var fieldGroup = entry.getValue();

        var jsonData = resolver.getJsonValue(entity, jsonField);
        if (jsonData == null) continue;

        deleteOwnedDescendants(fieldGroup, jsonData);
      }
    } finally {
      exit(ctx);
    }
  }

  private void handleSave(
      Model entity,
      String jsonField,
      List<MetaJsonField> fields,
      Map<String, Object> data,
      Map<String, Object> oldData) {

    validateReferencePayloads(fields, data);

    if (persistTransientChildren(fields, data)) {
      resolver.setJsonValue(entity, jsonField, data);
    }

    mergeExistingChildren(fields, data);

    if (oldData != null) {
      deleteOrphans(fields, oldData, data);
    }
  }

  @SuppressWarnings("unchecked")
  private void validateReferencePayloads(List<MetaJsonField> fields, Map<String, Object> data) {
    for (var field : fields) {
      if (canCascadePersist(field)) continue;

      var value = data.get(field.getName());
      if (value == null) continue;

      if (JsonReferenceResolver.isToManyType(field.getType())) {
        if (!(value instanceof List<?> items)) continue;
        for (var item : items) {
          if (item instanceof Map<?, ?> ref && isUnsaved((Map<String, Object>) ref)) {
            throwUnsavedReference(field);
          }
        }
      } else {
        if (value instanceof Map<?, ?> ref && isUnsaved((Map<String, Object>) ref)) {
          throwUnsavedReference(field);
        }
      }
    }
  }

  private boolean persistTransientChildren(List<MetaJsonField> fields, Map<String, Object> data) {
    var modified = false;

    for (var field : fields) {
      if (canCascadePersist(field) && cascadeSaveField(data, field)) {
        modified = true;
      }
    }

    return modified;
  }

  @SuppressWarnings("unchecked")
  private boolean cascadeSaveField(Map<String, Object> data, MetaJsonField field) {
    var value = data.get(field.getName());
    if (value == null) return false;

    if (JsonReferenceResolver.isToManyType(field.getType())) {
      if (!(value instanceof List<?> list)) return false;
      return cascadeSaveCollection(data, field, (List<Map<String, Object>>) list);
    } else {
      if (!(value instanceof Map<?, ?> map)) return false;
      return cascadeSaveSingle(data, field, (Map<String, Object>) map);
    }
  }

  private boolean cascadeSaveSingle(
      Map<String, Object> data, MetaJsonField field, Map<String, Object> ref) {
    if (!isUnsaved(ref)) return false;

    var saved = cascadeSaveRecord(field, ref);
    data.put(field.getName(), toReferenceMap(saved));
    return true;
  }

  private boolean cascadeSaveCollection(
      Map<String, Object> data, MetaJsonField field, List<Map<String, Object>> items) {
    var modified = false;
    var updatedItems = new ArrayList<Map<String, Object>>();

    for (var item : items) {
      if (isUnsaved(item)) {
        var saved = cascadeSaveRecord(field, item);
        updatedItems.add(toReferenceMap(saved));
        modified = true;
      } else {
        updatedItems.add(item);
      }
    }

    if (modified) {
      data.put(field.getName(), updatedItems);
    }
    return modified;
  }

  private Model cascadeSaveRecord(MetaJsonField field, Map<String, Object> data) {
    if (!JsonReferenceResolver.isJsonModelTarget(field)) {
      throwUnsavedReference(field);
    }

    var record = new MetaJsonRecord();
    record.setJsonModel(getTargetJsonModelName(field));
    record.setAttrs(JsonReferenceResolver.toJson(new HashMap<>(data)));
    var saved = JPA.save(record);

    processChildGraph(saved);
    refreshJsonRecordName(saved);

    return saved;
  }

  private void processChildGraph(MetaJsonRecord entity) {
    var ctx = SourceContext.of(entity);
    var fields = resolver.findReferenceFields(ctx);
    if (fields.isEmpty()) return;

    var grouped = groupByModelField(fields);
    for (var entry : grouped.entrySet()) {
      var jsonField = entry.getKey();
      var fieldGroup = entry.getValue();

      var jsonData = resolver.getJsonValue(entity, jsonField);
      if (jsonData == null) jsonData = Map.of();

      var mutableData = new HashMap<>(jsonData);
      if (persistTransientChildren(fieldGroup, mutableData)) {
        resolver.setJsonValue(entity, jsonField, mutableData);
        jsonData = mutableData;
      }

      mergeExistingChildren(fieldGroup, jsonData);
    }
  }

  @SuppressWarnings("unchecked")
  private void mergeExistingChildren(List<MetaJsonField> fields, Map<String, Object> data) {
    var childDataMap = new HashMap<Long, Map<String, Object>>();

    for (var field : fields) {
      if (!canCascadeMerge(field) || !JsonReferenceResolver.isJsonModelTarget(field)) continue;

      var value = data.get(field.getName());
      if (value == null) continue;

      if (JsonReferenceResolver.isToManyType(field.getType())) {
        if (!(value instanceof List<?> items)) continue;

        for (var item : items) {
          if (!(item instanceof Map)) continue;
          var itemData = (Map<String, Object>) item;
          collectChildData(itemData, childDataMap);
        }
      } else {
        if (!(value instanceof Map)) continue;
        collectChildData((Map<String, Object>) value, childDataMap);
      }
    }

    if (childDataMap.isEmpty()) return;

    var childIds = new ArrayList<>(childDataMap.keySet());
    var children =
        JPA.em()
            .createQuery("SELECT m FROM MetaJsonRecord m WHERE m.id IN :ids", MetaJsonRecord.class)
            .setParameter("ids", childIds)
            .getResultList();

    for (var child : children) {
      var childData = childDataMap.get(child.getId());
      if (childData != null) {
        var childCtx = SourceContext.of(child);
        if (!enter(childCtx)) continue;
        try {
          var oldAttrs = resolver.getJsonValue(child, "attrs");
          var updatedAttrs = patchChildAttrs(child, childData);
          updateChildReferences(child, oldAttrs, updatedAttrs);
          refreshJsonRecordName(child);
        } finally {
          exit(childCtx);
        }
      }
    }
  }

  private void updateChildReferences(
      MetaJsonRecord child, Map<String, Object> oldData, Map<String, Object> newData) {
    if (oldData == null || newData == null) return;

    var ctx = SourceContext.of(child);
    var fields = resolver.findReferenceFields(ctx);
    if (fields.isEmpty()) return;

    var grouped = groupByModelField(fields);
    for (var entry : grouped.entrySet()) {
      var jsonField = entry.getKey();
      var fieldGroup = entry.getValue();

      if (persistTransientChildren(fieldGroup, newData)) {
        resolver.setJsonValue(child, jsonField, newData);
      }

      mergeExistingChildren(fieldGroup, newData);
      deleteOrphans(fieldGroup, oldData, newData);
    }
  }

  private void collectChildData(
      Map<String, Object> itemData, Map<Long, Map<String, Object>> childDataMap) {
    if (isUnsaved(itemData)) return;
    if (!hasMergePayload(itemData)) return;

    var childId = JsonReferenceResolver.extractId(itemData);
    if (childId != null) {
      childDataMap.put(childId, itemData);
    }
  }

  private boolean hasMergePayload(Map<String, Object> itemData) {
    for (var key : itemData.keySet()) {
      if (!"id".equals(key) && !"version".equals(key) && !"$version".equals(key)) {
        return true;
      }
    }
    return false;
  }

  private void throwUnsavedReference(MetaJsonField field) {
    throw new IllegalStateException(
        String.format(
            "Field '%s': unsaved %s reference not allowed. Save the target entity first.",
            field.getName(), field.getTargetModel()));
  }

  private Map<String, Object> patchChildAttrs(MetaJsonRecord child, Map<String, Object> newData) {
    var currentAttrs = resolver.getJsonValue(child, "attrs");
    var mutableAttrs =
        currentAttrs == null ? new HashMap<String, Object>() : new HashMap<>(currentAttrs);
    for (var entry : newData.entrySet()) {
      var key = entry.getKey();
      if (!"id".equals(key) && !"version".equals(key) && !"$version".equals(key)) {
        mutableAttrs.put(key, entry.getValue());
      }
    }

    resolver.setJsonValue(child, "attrs", mutableAttrs);
    return mutableAttrs;
  }

  private String getTargetJsonModelName(MetaJsonField field) {
    var targetJsonModel = field.getTargetJsonModel();
    return targetJsonModel != null ? targetJsonModel.getName() : null;
  }

  private Map<String, Object> toReferenceMap(Model saved) {
    var ref = new HashMap<String, Object>();
    ref.put("id", saved.getId());

    if (saved instanceof MetaJsonRecord jsonRecord) {
      var name = jsonRecord.getName();
      if (name != null) {
        ref.put("name", name);
      }
    }
    return ref;
  }

  private void deleteOrphans(
      List<MetaJsonField> fields, Map<String, Object> oldData, Map<String, Object> newData) {
    var fieldsByName =
        fields.stream().collect(Collectors.toMap(MetaJsonField::getName, Function.identity()));

    for (var entry : fieldsByName.entrySet()) {
      var field = entry.getValue();
      if (!canCascadeRemove(field)) continue;

      var oldIds = new HashSet<>(resolver.extractReferenceIds(oldData, field));
      var newIds = new HashSet<>(resolver.extractReferenceIds(newData, field));
      oldIds.removeAll(newIds);

      if (oldIds.isEmpty()) continue;

      deleteEntities(field, oldIds);
    }
  }

  private void deleteOwnedDescendants(List<MetaJsonField> fields, Map<String, Object> data) {
    for (var field : fields) {
      if (!canCascadeRemove(field)) continue;
      var ids = new HashSet<>(resolver.extractReferenceIds(data, field));
      if (ids.isEmpty()) continue;
      deleteEntities(field, ids);
    }
  }

  @SuppressWarnings("unchecked")
  private void deleteEntities(MetaJsonField field, Set<Long> ids) {
    var targetModel = JsonReferenceResolver.resolveTargetModel(field);
    var modelClass = JsonReferenceResolver.findClass(targetModel);
    var modelRepo = (JpaRepository<Model>) JpaRepository.of(modelClass);
    var entities = modelRepo.findByIds(new ArrayList<>(ids));
    for (var entity : entities) {
      if (entity == null) continue;
      modelRepo.remove(entity);
    }
  }

  private boolean isUnsaved(Map<String, Object> ref) {
    var id = ref.get("id");
    if (id == null) return true;
    var idValue = id instanceof Number num ? num.longValue() : Long.parseLong(id.toString());
    return idValue <= 0;
  }

  private boolean hasJsonField(Model entity) {
    var entityClass = EntityHelper.getEntityClass(entity);
    var mapper = Mapper.of(entityClass);
    for (var property : mapper.getProperties()) {
      if (property.isJson()) return true;
    }
    return false;
  }

  private Map<String, List<MetaJsonField>> groupByModelField(List<MetaJsonField> fields) {
    return fields.stream().collect(Collectors.groupingBy(MetaJsonField::getModelField));
  }

  private boolean canCascadePersist(MetaJsonField field) {
    return JsonReferenceResolver.isJsonModelTarget(field);
  }

  private boolean canCascadeMerge(MetaJsonField field) {
    return JsonReferenceResolver.isJsonModelTarget(field);
  }

  private boolean canCascadeRemove(MetaJsonField field) {
    return isOwned(field);
  }

  private boolean isOwned(MetaJsonField field) {
    return JsonReferenceResolver.isOneToManyType(field.getType())
        && JsonReferenceResolver.isJsonModelTarget(field);
  }

  private void refreshJsonRecordName(MetaJsonRecord record) {
    var model =
        Query.of(MetaJsonModel.class)
            .filter("self.name = :name")
            .bind("name", record.getJsonModel())
            .autoFlush(false)
            .cacheable()
            .fetchOne();
    if (model == null || model.getNameField() == null) return;

    record.setName((String) new JsonContext(record).get(model.getNameField()));
  }

  private String entityKey(SourceContext ctx) {
    return ctx.model() + "#" + ctx.id();
  }

  private String snapshotKey(SourceContext ctx, String jsonField) {
    return entityKey(ctx) + "#" + jsonField;
  }

  private boolean enter(SourceContext ctx) {
    if (ctx.id() == null || ctx.id() <= 0) return true;
    var key = entityKey(ctx);
    var set = IN_PROGRESS.get();
    if (set.contains(key)) {
      return false;
    }
    set.add(key);
    return true;
  }

  private void exit(SourceContext ctx) {
    if (ctx.id() == null || ctx.id() <= 0) return;
    var key = entityKey(ctx);
    var set = IN_PROGRESS.get();
    set.remove(key);
    if (set.isEmpty()) {
      IN_PROGRESS.remove();
    }
  }
}
