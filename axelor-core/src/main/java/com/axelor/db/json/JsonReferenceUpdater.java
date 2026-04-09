/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.json;

import com.axelor.cache.AxelorCache;
import com.axelor.cache.CacheBuilder;
import com.axelor.db.EntityHelper;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.internal.DBHelper;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.PropertyType;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.repo.MetaJsonFieldRepository;
import jakarta.inject.Singleton;
import jakarta.persistence.Query;
import java.time.Duration;
import java.util.List;

@Singleton
public class JsonReferenceUpdater {

  private static final AxelorCache<String, List<MetaJsonField>> targetFieldCache =
      CacheBuilder.newBuilder("targetFieldCache")
          .expireAfterWrite(Duration.ofHours(1))
          .build(
              modelKey -> {
                var fieldRepository = Beans.get(MetaJsonFieldRepository.class);
                var filter =
                    modelKey.contains(".")
                        ? "self.type = 'many-to-one' and self.targetModel = :model"
                        : "self.type = 'json-many-to-one' and self.targetJsonModel.name = :model";
                return fieldRepository
                    .all()
                    .filter(filter)
                    .bind("model", modelKey)
                    .cacheable()
                    .fetch();
              });

  public static void clearCache(String targetModelKey) {
    if (targetModelKey != null) {
      targetFieldCache.invalidate(targetModelKey);
    }
  }

  public void afterSave(Model entity) {
    if (DBHelper.isHSQL()) return;

    var entityClass = EntityHelper.getEntityClass(entity);
    var mapper = Mapper.of(entityClass);
    var nameField = mapper.getNameField();

    if (nameField == null || nameField.getType() != PropertyType.STRING) return;
    if (entity.getId() == null || entity.getId() <= 0) return;

    var model = entity instanceof MetaJsonRecord rec ? rec.getJsonModel() : entityClass.getName();
    var fields = targetFieldCache.get(model);
    if (fields.isEmpty()) return;

    var nameValue = nameField.get(entity);
    var entityId = entity.getId().toString();

    for (var field : fields) {
      Query query =
          JPA.em()
              .createQuery(
                  "UPDATE %s self SET self.%s = json_set(self.%s, '%s.%s', :value) WHERE json_extract(self.%s, '%s', 'id') = :id"
                      .formatted(
                          field.getModel(),
                          field.getModelField(),
                          field.getModelField(),
                          field.getName(),
                          nameField.getName(),
                          field.getModelField(),
                          field.getName()));
      query.setParameter("value", nameValue);
      query.setParameter("id", entityId);
      query.executeUpdate();
    }
  }
}
