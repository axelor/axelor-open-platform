/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta;

import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonModel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.Transaction;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Invalidates {@link MetaStore} JSON-field caches once per transaction that changes a
 * cache-affecting entity.
 *
 * <p>When a transaction flushes its first cache-affecting entity, a single {@link
 * AfterTransactionCompletionProcess} is registered and the remaining rows are deduplicated via
 * {@link #PENDING}. This avoids invalidating the (possibly distributed) cache for every row. This
 * follows Hibernate's own {@link org.hibernate.cache.internal.CollectionCacheInvalidator}, which
 * registers its after-completion process from flush-time post events.
 *
 * <p>Changes to JSON fields and JSON models are only reflected by {@link MetaStore} once the
 * transaction completes. A transaction that changes them and reads them back before committing may
 * get the previously cached state.
 *
 * <p>The cache is invalidated on both commit and rollback: the cache loaders run on the caller's
 * session, so a cache miss during a transaction with flushed changes may have cached uncommitted
 * state.
 */
public class MetaStoreCacheInvalidator
    implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

  /** Transactions that already have a pending invalidation scheduled. */
  private static final Set<Transaction> PENDING = ConcurrentHashMap.newKeySet();

  @Override
  public void onPostInsert(PostInsertEvent event) {
    scheduleInvalidation(event.getSession(), event.getEntity());
  }

  @Override
  public void onPostUpdate(PostUpdateEvent event) {
    scheduleInvalidation(event.getSession(), event.getEntity());
  }

  @Override
  public void onPostDelete(PostDeleteEvent event) {
    scheduleInvalidation(event.getSession(), event.getEntity());
  }

  @Override
  public boolean requiresPostCommitHandling(EntityPersister persister) {
    // not used, only for POST_COMMIT_* group
    return true;
  }

  private void scheduleInvalidation(EventSource session, Object entity) {
    if (!(entity instanceof MetaJsonField || entity instanceof MetaJsonModel)) {
      return;
    }

    final Transaction transaction = session.accessTransaction();

    // Register the after-completion hook only once per transaction.
    if (!PENDING.add(transaction)) {
      return;
    }

    session
        .getActionQueue()
        .registerProcess(
            (AfterTransactionCompletionProcess)
                (success, s) -> {
                  PENDING.remove(transaction);
                  MetaStore.invalidateJsonFields();
                });
  }
}
