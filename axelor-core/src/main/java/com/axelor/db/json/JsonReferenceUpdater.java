/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.json;

import com.axelor.db.EntityHelper;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.internal.DBHelper;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.PropertyType;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaJsonRecord;
import jakarta.inject.Singleton;
import jakarta.persistence.Query;

@Singleton
public class JsonReferenceUpdater {

  public void afterSave(Model entity) {
    if (DBHelper.isHSQL()) return;

    var entityClass = EntityHelper.getEntityClass(entity);
    var mapper = Mapper.of(entityClass);
    var nameField = mapper.getNameField();

    if (nameField == null || nameField.getType() != PropertyType.STRING) return;
    if (entity.getId() == null || entity.getId() <= 0) return;

    var model = entity instanceof MetaJsonRecord rec ? rec.getJsonModel() : entityClass.getName();
    var fields = MetaStore.getTargetJsonFields(model);
    if (fields.isEmpty()) return;

    var nameValue = nameField.get(entity);
    var entityId = entity.getId().toString();

    for (var field : fields) {
      Query query =
          JPA.em()
              .createQuery(
                  "UPDATE %s self SET self.%s = json_set(self.%s, '%s.%s', :value) WHERE json_extract(self.%s, '%s', 'id') = :id"
                      .formatted(
                          field.model(),
                          field.modelField(),
                          field.modelField(),
                          field.name(),
                          nameField.getName(),
                          field.modelField(),
                          field.name()));
      query.setParameter("value", nameValue);
      query.setParameter("id", entityId);
      query.executeUpdate();
    }
  }
}
