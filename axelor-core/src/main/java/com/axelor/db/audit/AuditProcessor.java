/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.audit.db.AuditEventType;
import com.axelor.audit.db.AuditLog;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.audit.state.AuditState;
import com.axelor.db.audit.state.EntityState;
import com.axelor.db.mapper.Mapper;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.service.MailMessageTrackingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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
  private static final int BATCH_SIZE = 100;
  private static final int MAX_RETRY = 3;

  @Inject private MailMessageTrackingService service;
  @Inject private ObjectMapper objectMapper;

  // Throttling constants
  private static final long BATCH_DELAY_MS =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_BATCH_DELAY, 5);
  private static final long BUSY_BACKOFF_MS =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_BUSY_BACKOFF, 200);
  private static final long ACTIVITY_WINDOW_MS =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_ACTIVITY_WINDOW, 200);
  ; // Consider idle after 200ms of no activity

  // The last time activity was signaled
  private static volatile long lastActivityTime = 0;

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
  @Transactional
  public void process() {
    log.info("Recovering audit logs...");
    process(this::findPending);
  }

  /** Process audit logs for a specific transaction ID. */
  @Transactional
  public void process(String txId) {
    log.trace("Starting audit log processing for transaction ID: {}", txId);
    process(() -> findPending(txId));
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

  /** Core processing loop that fetches and processes audit logs in batches. */
  private void process(Supplier<List<AuditLog>> fetcher) {
    var totalProcessed = 0;
    var totalFailed = 0;

    while (true) {
      // Check if main work is active - back off immediately
      if (isSystemBusy()) {
        pause(BUSY_BACKOFF_MS);
        continue;
      }

      var batch = fetcher.get();
      if (batch.isEmpty()) {
        break;
      }

      var processedIds = new ArrayList<Long>();

      for (var auditLog : batch) {
        try {
          var ids = process(auditLog);
          processedIds.addAll(ids);
          totalProcessed++;
        } catch (Exception e) {
          totalFailed++;
          handleError(auditLog, e);
        }
      }

      // Flush pending changes (e.g., MailMessage inserts, error updates)
      JPA.flush();

      // Bulk delete successfully processed AuditLogs
      if (!processedIds.isEmpty()) {
        JPA.all(AuditLog.class).filter("self.id IN :ids").bind("ids", processedIds).delete();
      }

      // Clear to avoid memory issues
      JPA.clear();

      // Wait a bit before the next batch to prevent consuming 100% of resources (either CPU of DB
      // access)
      // so that other thread can also process. Especially in the case of massive audit log to
      // process.
      pause(BATCH_DELAY_MS);
    }

    log.trace(
        "Audit log processing complete. Processed: {}, Failed: {}", totalProcessed, totalFailed);
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

  private List<Long> process(AuditLog auditLog) throws Exception {
    // Check if already processed (might be consolidated with previous)
    if (Boolean.TRUE.equals(auditLog.getProcessed())) {
      return Collections.emptyList();
    }

    var txId = auditLog.getTxId();
    var relatedId = auditLog.getRelatedId();
    var relatedModel = auditLog.getRelatedModel();
    var eventType = auditLog.getEventType();

    log.trace("Processing audit logs for {}#{} in transaction {}", relatedModel, relatedId, txId);

    // Find all audit logs for same entity in same transaction
    var group = findRelated(txId, eventType, relatedId, relatedModel);
    if (group.isEmpty()) {
      return Collections.emptyList();
    }

    if (group.size() > 1) {
      log.trace("Consolidating {} audit logs", group.size());
    }

    // Consolidate ALL changes from all audit logs in this transaction
    var firstLog = group.getFirst();
    var lastLog = group.getLast();
    var oldValues = fromJSON(firstLog.getPreviousState());
    var values = fromJSON(lastLog.getCurrentState());

    // Process with consolidated state

    var entityClass = Class.forName(relatedModel).asSubclass(Model.class);
    var entity = JPA.em().find(entityClass, relatedId);

    // If entity is deleted, skip processing
    if (entity != null) {
      var entityState =
          new AuditState(
              lastLog.getCreatedOn(),
              eventType,
              new EntityState(
                  entity, parseValues(entityClass, values), parseValues(entityClass, oldValues)));
      // Process the audit log
      service.process(entityState, lastLog.getUser());
    }

    // Mark them processed
    for (var logEntry : group) {
      logEntry.setProcessed(true);
      logEntry.setProcessedOn(LocalDateTime.now());
    }

    // Return IDs of all audit logs in this group for deletion
    return group.stream().map(AuditLog::getId).toList();
  }

  private void handleError(AuditLog auditLog, Exception e) {
    log.error("Failed to process audit log: {}", auditLog.getId(), e);

    var message = e.getMessage();
    if (message != null && message.length() > 1000) {
      message = message.substring(0, 1000);
    }

    auditLog.setRetryCount(auditLog.getRetryCount() + 1);
    auditLog.setErrorMessage(message);

    if (auditLog.getRetryCount() >= MAX_RETRY) {
      log.error("Max retries exceeded for audit log: {}", auditLog.getId());
      auditLog.setProcessed(true); // Stop retrying
      auditLog.setProcessedOn(LocalDateTime.now());
    }
  }

  private List<AuditLog> findPending() {
    return JPA.all(AuditLog.class)
        .filter("self.processed = false AND COALESCE(self.retryCount, 0) < :maxRetry")
        .bind("maxRetry", MAX_RETRY)
        .order("txId")
        .order("relatedModel")
        .order("relatedId")
        .order("eventType")
        .order("createdOn")
        .fetch(BATCH_SIZE);
  }

  private List<AuditLog> findPending(String txId) {
    return JPA.all(AuditLog.class)
        .filter(
            "self.processed = false AND COALESCE(self.retryCount, 0) < :maxRetry "
                + "AND self.txId = :txId")
        .bind("maxRetry", MAX_RETRY)
        .bind("txId", txId)
        .order("relatedModel")
        .order("relatedId")
        .order("eventType")
        .order("createdOn")
        .fetch(BATCH_SIZE);
  }

  private List<AuditLog> findRelated(
      String txId, AuditEventType eventType, Long relatedId, String relatedModel) {
    return JPA.all(AuditLog.class)
        .filter(
            """
            self.txId = :txId
            AND self.processed = false
            AND self.eventType = :eventType
            AND self.relatedModel = :relatedModel
            AND self.relatedId = :relatedId
            """)
        .bind("txId", txId)
        .bind("relatedId", relatedId)
        .bind("relatedModel", relatedModel)
        .bind("eventType", eventType)
        .order("createdOn")
        .fetch();
  }

  private Map<String, Object> parseValues(Class<?> entityClass, Map<String, Object> values) {
    var mapper = Mapper.of(entityClass);
    var entity = Mapper.toBean(entityClass, null);
    var parsedValues = new HashMap<String, Object>();
    for (var entry : values.entrySet()) {
      var name = entry.getKey();
      var value = entry.getValue();
      var prop = mapper.getProperty(name);
      if (prop == null || prop.isReference()) {
        parsedValues.put(name, value);
        continue;
      }
      prop.set(entity, value);
      parsedValues.put(name, prop.get(entity));
    }
    return parsedValues;
  }

  /**
   * Deserializes a JSON string into a map of string keys to object values.
   *
   * @param json the JSON string to be deserialized
   * @return a map containing the deserialized key-value pairs; returns an empty map if
   *     deserialization fails or if the input is blank
   */
  private Map<String, Object> fromJSON(String json) {
    if (com.axelor.common.StringUtils.isBlank(json)) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      log.error("Failed to deserialize JSON", e);
      return Collections.emptyMap();
    }
  }
}
