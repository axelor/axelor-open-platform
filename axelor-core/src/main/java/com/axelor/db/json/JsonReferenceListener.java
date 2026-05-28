/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.json;

import com.axelor.db.EntityHelper;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonModel;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

public class JsonReferenceListener
    implements PreInsertEventListener,
        PreUpdateEventListener,
        PreDeleteEventListener,
        PostCommitUpdateEventListener {

  private JsonReferenceCascader jsonReferenceCascader;

  @Override
  public boolean onPreInsert(PreInsertEvent event) {
    invalidateCaches(event.getEntity());
    return false;
  }

  @Override
  public boolean onPreUpdate(PreUpdateEvent event) {
    invalidateCaches(event.getEntity());
    if (event.getEntity() instanceof Model model) {
      captureJsonOldState(model, event);
    }
    return false;
  }

  @Override
  public boolean onPreDelete(PreDeleteEvent event) {
    invalidateCaches(event.getEntity());
    return false;
  }

  private void invalidateCaches(Object entity) {
    if (entity instanceof MetaJsonField field) {
      JsonReferenceResolver.clearCache(
          Optional.ofNullable(field.getJsonModel())
              .map(MetaJsonModel::getName)
              .orElse(field.getModel()));
      JsonReferenceUpdater.clearCache(
          Optional.ofNullable(field.getTargetJsonModel())
              .map(MetaJsonModel::getName)
              .orElse(field.getTargetModel()));
    } else if (entity instanceof MetaJsonModel model) {
      JsonReferenceResolver.clearCache(model.getName());
      JsonReferenceUpdater.clearCache(model.getName());
    }
  }

  private void captureJsonOldState(Model model, PreUpdateEvent event) {
    if (!JsonReferenceCascader.hasJsonField(model)) return;

    var state = event.getState();
    var oldState = event.getOldState();
    if (state == null || oldState == null) return;

    var propNames = event.getPersister().getPropertyNames();
    var mapper = Mapper.of(EntityHelper.getEntityClass(model));
    var jsonManager = getJsonReferenceCascader();

    for (var i = 0; i < propNames.length; ++i) {
      if (Objects.equals(state[i], oldState[i])) continue;
      var prop = mapper.getProperty(propNames[i]);
      if (prop != null && prop.isJson() && oldState[i] instanceof String oldStr) {
        jsonManager.captureOldState(model, propNames[i], oldStr);
      }
    }
  }

  @Override
  public boolean requiresPostCommitHandling(EntityPersister persister) {
    return JsonReferenceCascader.hasJsonField(persister.getMappedClass());
  }

  @Override
  public void onPostUpdate(PostUpdateEvent event) {
    clearState(event);
  }

  @Override
  public void onPostUpdateCommitFailed(PostUpdateEvent event) {
    clearState(event);
  }

  private void clearState(PostUpdateEvent event) {
    if (event.getEntity() instanceof Model model) {
      getJsonReferenceCascader().clearSaveState(model);
    }
  }

  private JsonReferenceCascader getJsonReferenceCascader() {
    if (jsonReferenceCascader == null) {
      jsonReferenceCascader = Beans.get(JsonReferenceCascader.class);
    }
    return jsonReferenceCascader;
  }
}
