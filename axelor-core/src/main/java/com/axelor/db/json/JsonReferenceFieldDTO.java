/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.json;

import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonModel;
import com.axelor.meta.db.MetaJsonRecord;
import java.util.Optional;

public record JsonReferenceFieldDTO(
    String name,
    String type,
    String model,
    String modelField,
    String targetModel,
    String targetJsonModel) {

  public static JsonReferenceFieldDTO from(MetaJsonField field) {
    return new JsonReferenceFieldDTO(
        field.getName(),
        field.getType(),
        field.getModel(),
        field.getModelField(),
        field.getTargetModel(),
        Optional.ofNullable(field.getTargetJsonModel()).map(MetaJsonModel::getName).orElse(null));
  }

  public boolean isJsonModelTarget() {
    return targetModel == null || MetaJsonRecord.class.getName().equals(targetModel);
  }

  public String resolveTargetModel() {
    return targetModel != null ? targetModel : MetaJsonRecord.class.getName();
  }
}
