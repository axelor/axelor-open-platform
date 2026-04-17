/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.json;

import com.axelor.db.EntityHelper;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import java.util.Objects;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

public class JsonReferenceListener
    implements PreUpdateEventListener, PostCommitUpdateEventListener {
  private static final long serialVersionUID = 1L;

  private JsonReferenceCascader jsonReferenceCascader;

  @Override
  public boolean onPreUpdate(PreUpdateEvent event) {
    if (event.getEntity() instanceof Model model) {
      captureJsonOldState(model, event);
    }

    return false;
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
