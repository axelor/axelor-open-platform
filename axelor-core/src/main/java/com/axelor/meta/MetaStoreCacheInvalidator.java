/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta;

import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonModel;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.MetaView;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Invalidates {@link MetaStore} JSON-field caches after the triggering transaction commits.
 *
 * <p>Caches are also invalidated when the commit fails: the cache loaders run on the caller's
 * session, so a read performed during the failed transaction may have cached uncommitted state.
 */
public class MetaStoreCacheInvalidator
    implements PostCommitInsertEventListener,
        PostCommitUpdateEventListener,
        PostCommitDeleteEventListener {

  @Override
  public boolean requiresPostCommitHandling(EntityPersister persister) {
    return affectsJsonFieldsCache(persister.getMappedClass());
  }

  @Override
  public void onPostInsert(PostInsertEvent event) {
    invalidate(event.getEntity());
  }

  @Override
  public void onPostInsertCommitFailed(PostInsertEvent event) {
    invalidate(event.getEntity());
  }

  @Override
  public void onPostUpdate(PostUpdateEvent event) {
    invalidate(event.getEntity());
  }

  @Override
  public void onPostUpdateCommitFailed(PostUpdateEvent event) {
    invalidate(event.getEntity());
  }

  @Override
  public void onPostDelete(PostDeleteEvent event) {
    invalidate(event.getEntity());
  }

  @Override
  public void onPostDeleteCommitFailed(PostDeleteEvent event) {
    invalidate(event.getEntity());
  }

  private static void invalidate(Object entity) {
    // Need to recheck it affects json fields cache,
    // because `requiresPostCommitHandling()` is an optimization flag only.
    if (entity != null && affectsJsonFieldsCache(entity.getClass())) {
      MetaStore.invalidateJsonFields();
    }
  }

  private static boolean affectsJsonFieldsCache(Class<?> klass) {
    return MetaJsonField.class.isAssignableFrom(klass)
        || MetaJsonModel.class.isAssignableFrom(klass)
        || MetaSelect.class.isAssignableFrom(klass) // See MetaSelectItem#select
        || MetaSelectItem.class.isAssignableFrom(klass) // See MetaStore#buildSelectionMap
        || MetaView.class.isAssignableFrom(klass); // view names in MetaStore#updateJsonFields
  }
}
