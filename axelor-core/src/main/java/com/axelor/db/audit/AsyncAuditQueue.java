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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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

  private final AtomicLong failureCounter = new AtomicLong(0);
  private static final long SHUTDOWN_TIMEOUT_SECONDS = 30;

  private static final ThreadPoolExecutor POOL =
      new ThreadPoolExecutor(
          1,
          1,
          0L,
          TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<>(),
          (task) -> {
            var thread = new Thread(task);
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setName("Audit-Worker");
            return thread;
          });

  @Override
  public void process(String txId) {
    log.trace("Enqueue audit log processing for transaction ID: {}", txId);
    Runnable task =
        ContextAware.of()
            .withTransaction(false)
            .build(
                () -> {
                  try {
                    Beans.get(AuditProcessor.class).process(txId);
                  } catch (Exception e) {
                    failureCounter.incrementAndGet();
                    log.error("Error in audit log processing for transaction ID: {}", txId);
                  }
                });

    if (!POOL.isShutdown()) {
      POOL.execute(task);
    }
  }

  @Override
  public QueueStats getStatistics() {
    return new QueueStats(
        POOL.getQueue().size(),
        POOL.getCompletedTaskCount(),
        POOL.getActiveCount() > 0,
        failureCounter.get());
  }

  /**
   * Lifecycle listener that shuts down the audit queue when the application stops.
   *
   * <p>It attempts to wait up to {@value #SHUTDOWN_TIMEOUT_SECONDS} seconds for existing tasks to
   * complete before forcing a shutdown.
   *
   * @param event the application shutdown event.
   */
  public void onAppShutdown(@Observes ShutdownEvent event) {
    log.info("Shutting down AuditQueue...");

    POOL.shutdown();

    try {
      if (!POOL.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        log.debug("Audit queue did not terminate. Forcing shutdown...");
        POOL.shutdownNow();
      }
    } catch (InterruptedException e) {
      POOL.shutdownNow();
    }
  }
}
