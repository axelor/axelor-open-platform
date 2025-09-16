/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.db.repo;

import com.axelor.db.EntityHelper;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.internal.DBHelper;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.axelor.event.Observes;
import com.axelor.events.PostRequest;
import com.axelor.events.RequestEvent;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.rpc.Request;
import com.google.inject.persist.Transactional;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
public class MetaJsonReferenceUpdater {

  void handleOnSave(@Observes @Named(RequestEvent.SAVE) PostRequest event) {

    // Not supported on Oracle
    if (DBHelper.isOracle()) {
      return;
    }

    final Request request = event.getRequest();
    if (request.getBeanClass() == null) {
      return;
    }

    final Class<? extends Model> beanClass = request.getBeanClass().asSubclass(Model.class);
    final Mapper mapper = Mapper.of(beanClass);
    final Property field = mapper.getNameField();

    if (field == null) {
      return;
    }

    List<Object> items = request.getRecords();
    if (items == null) {
      items = new ArrayList<>();
      if (request.getData() != null) {
        items.add(request.getData());
      }
    }

    if (items.isEmpty()) {
      return;
    }

    List<? extends Model> records =
        items.stream()
            .map(Map.class::cast)
            .filter(map -> map.get("id") != null)
            .filter(map -> nameChanged(field, map))
            .map(map -> Long.parseLong(map.get("id").toString()))
            .map(id -> JPA.em().find(beanClass, id))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    updateJsonFields(records);
  }

  private boolean nameChanged(Property field, Map<?, ?> map) {
    if (field.isVirtual()) {
      Mapper mapper = Mapper.of(field.getEntity());
      return mapper.getComputeDependencies(field).stream().anyMatch(map::containsKey);
    }
    return map.containsKey(field.getName());
  }

  @Transactional
  public <T extends Model> void updateJsonFields(T record) {
    updateJsonFields(Collections.singletonList(record));
  }

  @Transactional
  public <T extends Model> void updateJsonFields(List<T> records) {
    if (records == null || records.isEmpty()) {
      return;
    }

    final Mapper beanMapper = Mapper.of(EntityHelper.getEntityClass(records.getFirst()));
    final Property field = beanMapper.getNameField();

    if (field == null || field.getType() != PropertyType.STRING) {
      return;
    }

    for (T bean : records) {
      updateJsonFields(bean, field);
    }
  }

  private <T extends Model> void updateJsonFields(T bean, Property nameField) {
    Class<T> entityClass = EntityHelper.getEntityClass(bean);
    String filter = "self.type = 'many-to-one' and self.targetModel = :model";
    String model = entityClass.getName();

    if (bean instanceof MetaJsonRecord metaJsonRecord) {
      filter = "self.type = 'json-many-to-one' and self.targetJsonModel.name = :model";
      model = metaJsonRecord.getJsonModel();
    }

    final MetaJsonFieldRepository fieldRepository = Beans.get(MetaJsonFieldRepository.class);
    final List<MetaJsonField> fields =
        fieldRepository.all().filter(filter).bind("model", model).fetch();

    for (MetaJsonField field : fields) {
      String queryString =
          "UPDATE %s self SET self.%s = json_set(self.%s, '%s.%s', :value) WHERE json_extract(self.%s, '%s', 'id') = :id"
              .formatted(
                  field.getModel(),
                  field.getModelField(),
                  field.getModelField(),
                  field.getName(),
                  nameField.getName(),
                  field.getModelField(),
                  field.getName());

      Query query = JPA.em().createQuery(queryString);
      query.setParameter("value", nameField.get(bean));
      query.setParameter("id", bean.getId().toString());
      query.executeUpdate();
    }
  }
}
