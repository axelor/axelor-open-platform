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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronous implementation of {@link AuditQueue} that offloads processing to a background
 * thread.
 *
 * <p>This class uses a single-threaded {@link ExecutorService} to process audit logs sequentially
 * (FIFO) without blocking the main application thread.
 */
@Singleton
class AsyncAuditQueue implements AuditQueue {

  private static final Logger log = LoggerFactory.getLogger(AsyncAuditQueue.class);

  private static final ExecutorService POOL =
      Executors.newSingleThreadExecutor(
          (task) -> {
            var thread = new Thread(task);
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setName("Audit-Worker");
            return thread;
          });

  @Override
  public void process(String txId) {
    Runnable task =
        ContextAware.of()
            .withTransaction(false)
            .build(
                () -> {
                  try {
                    Beans.get(AuditProcessor.class).process(txId);
                  } catch (Exception e) {
                    log.error("Starting audit log processing for transaction ID: {}", txId);
                  }
                });

    if (!POOL.isShutdown()) {
      POOL.execute(task);
    }
  }

  public void onAppShutdown(@Observes ShutdownEvent event) {
    log.info("Shutting down AuditQueue...");

    POOL.shutdown();

    try {
      if (!POOL.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
        log.debug("Audit queue did not terminate. Forcing shutdown...");
        POOL.shutdownNow();
      }
    } catch (InterruptedException e) {
      POOL.shutdownNow();
    }
  }
}
