/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.db.repo;

import com.axelor.db.json.JsonReferenceCascader;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonModel;
import java.util.Optional;

public class MetaJsonFieldRepository extends AbstractMetaJsonFieldRepository {

  @Override
  public MetaJsonField save(MetaJsonField entity) {
    invalidateCache(entity);
    return super.save(entity);
  }

  @Override
  public void remove(MetaJsonField entity) {
    invalidateCache(entity);
    super.remove(entity);
  }

  private void invalidateCache(MetaJsonField entity) {
    var modelKey =
        Optional.ofNullable(entity)
            .map(MetaJsonField::getJsonModel)
            .map(MetaJsonModel::getName)
            .orElse(entity != null ? entity.getModel() : null);

    JsonReferenceCascader.clearCache(modelKey);
  }
}
