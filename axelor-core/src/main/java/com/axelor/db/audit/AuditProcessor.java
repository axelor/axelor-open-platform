/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.audit.db.AuditLog;
import com.axelor.db.JPA;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailMessage;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for processing AuditLog.
 *
 * <p>This processor runs asynchronously (triggered by {@link AsyncAuditQueue}) and processes logs
 * in small batches. It employs a "Back-Pressure" mechanism to ensure it does not compete with
 * active user transactions for database resources.
 */
public class AuditProcessor {

  private static final Logger log = LoggerFactory.getLogger(AuditProcessor.class);
  private final int BATCH_SIZE =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_BATCH_SIZE, 100);

  private final AuditBatchProcessor batchProcessor;
  private BooleanSupplier keepRunningSupplier;

  // Throttling constants
  private final long BATCH_DELAY_MS =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_BATCH_DELAY, 5);
  private final long BUSY_BACKOFF_INTERVAL =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_BUSY_BACKOFF_INTERVAL, 200);
  private final long BUSY_BACKOFF_MAX_RETRIES =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_BUSY_BACKOFF_MAX_RETRIES, 3);
  private final long ACTIVITY_WINDOW_MS =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_ACTIVITY_WINDOW, 200);

  // Recovery leaves the most recent audit logs to the asynchronous queue
  private static final long RECOVERY_DELAY_SECONDS = 60 * 4;

  // The last time activity was signaled
  private static volatile long lastActivityTime = 0;

  public AuditProcessor() {
    this.batchProcessor = new AuditBatchProcessor(BATCH_SIZE);
  }

  public AuditProcessor(BooleanSupplier keepRunningSupplier) {
    this();
    this.keepRunningSupplier = keepRunningSupplier;
  }

  /**
   * Signal that entity tracking is happening (called from AuditTracker). This tells the processor
   * to back off as real work is in progress.
   */
  public static void signalActivity(Object value) {
    if (value instanceof MailMessage
        || value instanceof MailFollower
        || value instanceof AuditLog) {
      return; // Ignore audit & mail entities
    }
    lastActivityTime = System.currentTimeMillis();
  }

  /** Process all pending audit logs. */
  public void process() {
    String lastTxId = null;
    List<String> candidateTxIds;

    while (!(candidateTxIds = fetchCandidateTxIds(lastTxId, BATCH_SIZE)).isEmpty()) {
      if (lastTxId == null) {
        log.info("Recovering audit logs...");
      }

      for (String txId : candidateTxIds) {
        if (isShutdownRequest()) {
          return;
        }
        // Delegate to the specific processor
        process(txId);
      }

      lastTxId = candidateTxIds.getLast();
    }
  }

  /** Process audit logs for a specific transaction ID. */
  public void process(String txId) {
    log.trace("Starting audit log processing for transaction ID: {}", txId);
    processPendingWork(txId);
  }

  /**
   * Introduces a delay in execution for a specified duration.
   *
   * @param ms the time to pause in milliseconds
   */
  private void pause(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Determines if the system should shut down based on specific conditions: whether the {@code
   * keepRunningSupplier} is provided and returns {@code false}, or if the current thread has been
   * interrupted.
   *
   * @return true if a shutdown has been requested; false otherwise.
   */
  private boolean isShutdownRequest() {
    return (keepRunningSupplier != null && !keepRunningSupplier.getAsBoolean())
        || Thread.currentThread().isInterrupted();
  }

  /** Core processing loop that fetches and processes audit logs in batches. */
  private void processPendingWork(String txId) {
    long lastAuditLogId = 0;
    int totalProcessed = 0;
    int totalFailed = 0;

    int busyWaitCount = 0;

    while (true) {
      // Check for shutdown
      if (isShutdownRequest()) {
        break;
      }

      // Check if main work is active - back off immediately
      if (isSystemBusy() && busyWaitCount < BUSY_BACKOFF_MAX_RETRIES) {
        busyWaitCount++;
        pause(BUSY_BACKOFF_INTERVAL);
        continue;
      }
      busyWaitCount = 0;

      // Process batch
      BatchResult result;
      try {
        result = batchProcessor.processNextBatch(txId, lastAuditLogId);
      } catch (Exception e) {
        // Not caused by a specific group (e.g. deleting processed logs or commit)
        log.error("Unexpected error processing txId: {}", txId, e);
        break;
      }
      totalProcessed += result.processed();
      totalFailed += result.failed();

      // Move cursor past this batch
      lastAuditLogId = result.lastAuditLogId();

      // Everything processed, exit
      if (!result.hasMore()) {
        break;
      }

      // Check for shutdown without waiting
      if (isShutdownRequest()) {
        break;
      }

      // Wait a bit before the next batch to prevent consuming 100% of resources (either CPU of DB
      // access)
      // so that other thread can also process. Especially in the case of massive audit log to
      // process.
      pause(BATCH_DELAY_MS);
    }

    log.trace(
        "Audit log processing complete for transaction {}. Processed: {}, Failed: {}",
        txId,
        totalProcessed,
        totalFailed);
  }

  /**
   * Determines whether the system is currently busy based on the recent activity in entity
   * tracking. Compares the time elapsed since the last recorded activity with a predefined activity
   * window.
   *
   * <p>To prioritize the user's transaction over background work
   *
   * @return true if the system is considered busy, false otherwise
   */
  private boolean isSystemBusy() {
    return (System.currentTimeMillis() - lastActivityTime) < ACTIVITY_WINDOW_MS;
  }

  /**
   * Retrieves a list of candidate transaction IDs from the audit log table that have not been
   * processed, using keyset pagination on the transaction ID.
   *
   * <p>Transaction IDs are UUID v7, so ordering them also orders transactions by creation time.
   *
   * <p>Only audit logs older than the recovery delay are considered: the most recent ones are being
   * processed by the asynchronous queue, and processing them here too would only compete for the
   * same records.
   *
   * @param afterTxId the last transaction ID of the previous page; if null, starts from the first
   * @param limit the maximum number of transaction IDs to fetch from the database
   * @return a list of unprocessed transaction IDs greater than {@code afterTxId}, ordered by
   *     transaction ID
   */
  private List<String> fetchCandidateTxIds(String afterTxId, int limit) {
    String sql =
        """
          SELECT DISTINCT tx_id
          FROM audit_log
          WHERE processed = false
          AND created_on < ?
          %s
          ORDER BY tx_id
          LIMIT ?
          """
            .formatted(afterTxId == null ? "" : "AND tx_id > ?");

    LocalDateTime createdBefore = LocalDateTime.now().minusSeconds(RECOVERY_DELAY_SECONDS);
    List<String> result = new ArrayList<>();

    JPA.JDBCWork work =
        conn -> {
          try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setObject(idx++, createdBefore);
            if (afterTxId != null) {
              ps.setString(idx++, afterTxId);
            }
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
              while (rs.next()) {
                result.add(rs.getString(1));
              }
            }
          }
        };

    JPA.runInTransaction(() -> JPA.jdbcWork(work));

    return result;
  }
}
