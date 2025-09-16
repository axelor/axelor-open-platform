/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.db.Model;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.Transaction;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;

public class AuditTrail
    implements PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener {

  private static final long serialVersionUID = 1L;

  private final Map<Transaction, AuditTracker> trackers = new ConcurrentHashMap<>();

  private AuditTracker get(EventSource sourceSession) {
    return trackers.computeIfAbsent(
        sourceSession.accessTransaction(),
        transaction -> {
          final AuditTracker tracker = new AuditTracker();
          sourceSession
              .getActionQueue()
              .registerProcess(
                  (BeforeTransactionCompletionProcess)
                      session ->
                          Optional.ofNullable(trackers.get(transaction))
                              .ifPresent(x -> x.doBeforeTransactionCompletion(session)));
          sourceSession
              .getActionQueue()
              .registerProcess(
                  (AfterTransactionCompletionProcess)
                      (success, session) -> trackers.remove(transaction));
          return tracker;
        });
  }

  @Override
  public boolean onPreInsert(PreInsertEvent event) {
    final EventSource session = event.getSession();
    final AuditTracker tracker = get(session);

    if (event.getEntity() instanceof Model) {
      final Model entity = (Model) event.getEntity();
      final String[] names = event.getPersister().getPropertyNames();
      final Object[] state = event.getState();
      tracker.track(entity, names, state, null);
      tracker.updated(entity);
    }

    return false;
  }

  @Override
  public boolean onPreUpdate(PreUpdateEvent event) {
    final EventSource session = event.getSession();
    final AuditTracker tracker = get(session);

    if (event.getEntity() instanceof Model) {
      final Model entity = (Model) event.getEntity();
      final String[] names = event.getPersister().getPropertyNames();
      final Object[] state = event.getState();
      final Object[] oldState = event.getOldState();
      tracker.track(entity, names, state, oldState);
      tracker.updated(entity);
    }

    return false;
  }

  @Override
  public boolean onPreDelete(PreDeleteEvent event) {
    final EventSource session = event.getSession();
    final AuditTracker tracker = get(session);

    if (event.getEntity() instanceof Model) {
      final Model entity = (Model) event.getEntity();
      tracker.deleted(entity);
    }

    return false;
  }
}
