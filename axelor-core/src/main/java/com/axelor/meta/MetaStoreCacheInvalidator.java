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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.Transaction;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Invalidates {@link MetaStore} JSON-field caches once per transaction that changes a
 * cache-affecting entity.
 *
 * <p>The post-commit events fire once per changed row; to avoid invalidating the (possibly
 * distributed) cache for every row, the first cache-affecting entity of a transaction registers a
 * single {@link AfterTransactionCompletionProcess} and the remaining rows are deduplicated via
 * {@link #scheduled}.
 *
 * <p>The cache is invalidated on both commit and rollback: the cache loaders run on the caller's
 * session, so a read performed during a failed transaction may have cached uncommitted state.
 */
public class MetaStoreCacheInvalidator
    implements PostCommitInsertEventListener,
        PostCommitUpdateEventListener,
        PostCommitDeleteEventListener {

  /** Transactions that already have a pending invalidation scheduled. */
  private final Set<Transaction> scheduled = ConcurrentHashMap.newKeySet();

  @Override
  public boolean requiresPostCommitHandling(EntityPersister persister) {
    return affectsJsonFieldsCache(persister.getMappedClass());
  }

  @Override
  public void onPostInsert(PostInsertEvent event) {
    scheduleInvalidation(event.getSession(), event.getPersister());
  }

  @Override
  public void onPostInsertCommitFailed(PostInsertEvent event) {
    scheduleInvalidation(event.getSession(), event.getPersister());
  }

  @Override
  public void onPostUpdate(PostUpdateEvent event) {
    scheduleInvalidation(event.getSession(), event.getPersister());
  }

  @Override
  public void onPostUpdateCommitFailed(PostUpdateEvent event) {
    scheduleInvalidation(event.getSession(), event.getPersister());
  }

  @Override
  public void onPostDelete(PostDeleteEvent event) {
    scheduleInvalidation(event.getSession(), event.getPersister());
  }

  @Override
  public void onPostDeleteCommitFailed(PostDeleteEvent event) {
    scheduleInvalidation(event.getSession(), event.getPersister());
  }

  private void scheduleInvalidation(EventSource session, EntityPersister persister) {
    if (!affectsJsonFieldsCache(persister.getMappedClass())) {
      return;
    }

    var transaction = session.accessTransaction();
    // Register the after-completion hook only once per transaction.
    if (!scheduled.add(transaction)) {
      return;
    }

    session
        .getActionQueue()
        .registerProcess(
            (AfterTransactionCompletionProcess)
                (success, s) -> {
                  scheduled.remove(transaction);
                  MetaStore.invalidateJsonFields();
                });
  }

  private static boolean affectsJsonFieldsCache(Class<?> klass) {
    return MetaJsonField.class.isAssignableFrom(klass)
        || MetaJsonModel.class.isAssignableFrom(klass)
        || MetaSelect.class.isAssignableFrom(klass) // See MetaSelectItem#select
        || MetaSelectItem.class.isAssignableFrom(klass) // See MetaStore#buildSelectionMap
        || MetaView.class.isAssignableFrom(klass); // view names in MetaStore#updateJsonFields
  }
}
