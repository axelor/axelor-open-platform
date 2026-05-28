/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.concurrent.ContextAware;
import com.axelor.event.Observes;
import com.axelor.events.ShutdownEvent;
import jakarta.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
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
  private volatile boolean isActive = true;
  private static final long SHUTDOWN_TIMEOUT_SECONDS = 20;

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
                    AuditProcessor processor = new AuditProcessor(() -> isActive);
                    processor.process(txId);
                  } catch (Exception e) {
                    failureCounter.incrementAndGet();
                    log.error("Error in audit log processing for transaction ID: {}", txId);
                  }
                });

    if (!POOL.isShutdown()) {
      try {
        POOL.execute(task);
      } catch (RejectedExecutionException ignore) {
      }
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
   * <p>Shutdown as smoothly as possible: first stop accepting new tasks then wait up to {@value
   * #SHUTDOWN_TIMEOUT_SECONDS} seconds for existing tasks to complete. If tasks are still running
   * trigger signal to gracefully quit the process, then drain the queue.
   *
   * @param event the application shutdown event.
   */
  public void onAppShutdown(@Observes ShutdownEvent event) {
    log.info("Shutting down AuditQueue...");

    // Stop accepting new tasks
    POOL.shutdown();

    try {
      if (!POOL.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        log.debug("Audit queue did not terminate. Forcing shutdown...");
        // Trigger signal for active tasks to stop
        this.isActive = false;
        // Drain queue and interrupt active task
        POOL.shutdownNow();
      }
    } catch (InterruptedException e) {
      this.isActive = false;
      POOL.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
