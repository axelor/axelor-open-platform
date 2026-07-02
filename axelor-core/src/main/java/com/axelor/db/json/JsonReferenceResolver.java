/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.json;

import com.axelor.db.EntityHelper;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaJsonRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Singleton
class JsonReferenceResolver {

  private static final String ONE_TO_MANY = "one-to-many";
  private static final String JSON_ONE_TO_MANY = "json-one-to-many";

  public record SourceContext(String model, Long id, String jsonModel) {
    public static SourceContext of(Model entity) {
      var entityClass = EntityHelper.getEntityClass(entity);
      var jsonModel = entity instanceof MetaJsonRecord rec ? rec.getJsonModel() : null;
      return new SourceContext(entityClass.getName(), entity.getId(), jsonModel);
    }
  }

  public List<JsonReferenceFieldDTO> findReferenceFields(SourceContext ctx) {
    var modelKey = ctx.jsonModel() != null ? ctx.jsonModel() : ctx.model();
    return MetaStore.getReferenceJsonFields(modelKey);
  }

  public Map<String, Object> getJsonValue(Model entity, String jsonField) {
    var beanClass = EntityHelper.getEntityClass(entity);
    var mapper = Mapper.of(beanClass);
    var value = mapper.get(entity, jsonField);
    if (value == null) return null;
    return parseJson((String) value);
  }

  public void setJsonValue(Model entity, String jsonField, Map<String, Object> data) {
    var beanClass = EntityHelper.getEntityClass(entity);
    var mapper = Mapper.of(beanClass);
    mapper.set(entity, jsonField, toJson(data));
  }

  public static Map<String, Object> parseJson(String value) {
    try {
      var mapper = Beans.get(ObjectMapper.class);
      var type = mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
      return mapper.readValue(value, type);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Invalid JSON data", e);
    }
  }

  public static String toJson(Map<String, Object> data) {
    try {
      return Beans.get(ObjectMapper.class).writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize JSON", e);
    }
  }

  public static Long extractId(Map<?, ?> item) {
    var id = item.get("id");
    if (id == null) return null;
    var idValue = id instanceof Number num ? num.longValue() : Long.parseLong(id.toString());
    return idValue > 0 ? idValue : null;
  }

  public List<Long> extractReferenceIds(Map<?, ?> attrs, JsonReferenceFieldDTO field) {
    return isToManyType(field.type())
        ? extractCollectionIds(attrs, field.name())
        : extractSingleId(attrs, field.name());
  }

  private static List<Long> extractSingleId(Map<?, ?> attrs, String fieldName) {
    var value = attrs.get(fieldName);
    if (value == null) return List.of();
    if (value instanceof Map<?, ?> map) {
      var id = extractId(map);
      if (id != null) return List.of(id);
    }
    return List.of();
  }

  private static List<Long> extractCollectionIds(Map<?, ?> attrs, String fieldName) {
    var value = attrs.get(fieldName);
    if (value == null) return List.of();
    if (value instanceof List<?> list) {
      return list.stream()
          .filter(Map.class::isInstance)
          .map(Map.class::cast)
          .map(JsonReferenceResolver::extractId)
          .filter(Objects::nonNull)
          .toList();
    }
    return List.of();
  }

  public static Class<? extends Model> findClass(String name) {
    try {
      return Class.forName(name).asSubclass(Model.class);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Class not found: " + name, e);
    }
  }

  public static boolean isToManyType(String type) {
    return type != null && type.endsWith("-to-many");
  }

  public static boolean isOneToManyType(String type) {
    return ONE_TO_MANY.equals(type) || JSON_ONE_TO_MANY.equals(type);
  }
}
