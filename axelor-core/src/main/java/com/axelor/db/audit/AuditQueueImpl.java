/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.concurrent.ContextAware;
import com.axelor.event.Observes;
import com.axelor.events.ShutdownEvent;
import com.axelor.inject.Beans;
import jakarta.inject.Singleton;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class AuditQueueImpl implements AuditQueue {

  private static final Logger log = LoggerFactory.getLogger(AuditQueueImpl.class);

  private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
  private static final Queue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();
  private static final Executor POOL =
      Executors.newSingleThreadExecutor(
          (task) -> {
            var thread = new Thread(task);
            thread.setDaemon(true);
            thread.setName("Audit-Worker");
            return thread;
          });

  @Override
  public void process(String txId) {
    submit(
        ContextAware.of()
            .withTransaction(false)
            .build(() -> Beans.get(AuditProcessor.class).process(txId)));
  }

  private void submit(Runnable task) {
    QUEUE.add(task);
    if (RUNNING.getAndSet(true)) {
      return;
    }
    POOL.execute(
        () -> {
          Runnable next;
          while ((next = QUEUE.poll()) != null) {
            try {
              next.run();
            } catch (Exception e) {
              log.error("Error processing audit task", e);
            }
          }
          RUNNING.set(false);
        });
  }

  public void onAppShutdown(@Observes ShutdownEvent event) {
    log.info("Shutting down AuditQueue...");

    // Clear queued items first
    QUEUE.clear();

    // Wait for currently running task to finish (if any)
    final long timeout = 30_000; // 30 seconds
    final long startTime = System.currentTimeMillis();
    while (RUNNING.get()) {
      if (System.currentTimeMillis() - startTime > timeout) {
        log.warn("Timeout waiting for current audit task to complete");
        break;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  public static class Noop implements AuditQueue {

    @Override
    public void process(String txId) {
      // No operation
    }
  }
}
