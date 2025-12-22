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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.hibernate.Session;
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
  public void process() {
    log.info("Recovering audit logs...");
    processPendingWork(null);
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

  /** Core processing loop that fetches and processes audit logs in batches. */
  @Transactional
  protected void processPendingWork(String txId) {
    var totalProcessed = 0;
    var totalFailed = 0;

    while (true) {
      // Check if main work is active - back off immediately
      if (isSystemBusy()) {
        pause(BUSY_BACKOFF_MS);
        continue;
      }

      List<AuditWorkGroup> batch = fetchNextBatch(txId);
      if (batch.isEmpty()) {
        break;
      }

      var processedIds = new ArrayList<Long>();

      for (AuditWorkGroup auditWorkGroup : batch) {
        List<AuditLog> auditLogs = auditWorkGroup.fetchLogs();
        try {
          process(auditWorkGroup, auditLogs);
          processedIds.addAll(auditLogs.stream().map(AuditLog::getId).toList());
          totalProcessed += auditLogs.size();
        } catch (Exception e) {
          totalFailed += auditLogs.size();
          handleError(auditWorkGroup, auditLogs, e);
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

  private void process(AuditWorkGroup group, List<AuditLog> logs) throws Exception {

    // Check if already processed
    if (logs.isEmpty()) {
      return;
    }

    log.trace("Processing audit logs for {} ", group);

    if (logs.size() > 1) {
      log.trace("Consolidating {} audit logs for {}", logs.size(), group);
    }

    // Consolidate ALL changes from all audit logs in this transaction
    var firstLog = logs.getFirst();
    var lastLog = logs.getLast();
    var oldValues = fromJSON(firstLog.getPreviousState());
    var values = fromJSON(lastLog.getCurrentState());

    // Process with consolidated state
    var entityClass = Class.forName(group.getRelatedModel()).asSubclass(Model.class);
    var entity = JPA.em().find(entityClass, group.getRelatedId());

    // If entity is deleted, skip processing
    if (entity != null) {
      var entityState =
          new AuditState(
              lastLog.getCreatedOn(),
              group.getEventType(),
              new EntityState(
                  entity, parseValues(entityClass, values), parseValues(entityClass, oldValues)));
      // Process the audit log
      service.process(entityState, lastLog.getUser());
    }

    // Mark them processed
    logs.forEach(
        l -> {
          l.setProcessed(true);
          l.setProcessedOn(LocalDateTime.now());
        });
  }

  private void handleError(AuditWorkGroup group, List<AuditLog> logs, Exception e) {
    log.error("Failed to process audit logs for {}", group, e);

    var message = e.getMessage();
    if (message != null && message.length() > 1000) {
      message = message.substring(0, 1000);
    }

    for (AuditLog auditLog : logs) {
      auditLog.setRetryCount(auditLog.getRetryCount() + 1);
      auditLog.setErrorMessage(message);

      if (auditLog.getRetryCount() >= MAX_RETRY) {
        log.error("Max retries exceeded for audit log: {}", auditLog.getId());
        auditLog.setProcessed(true); // Stop retrying
        auditLog.setProcessedOn(LocalDateTime.now());
      }
    }
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

  private List<AuditWorkGroup> fetchNextBatch(String txId) {
    String sql =
        """
        SELECT tx_id, related_model, related_id, event_type
        FROM audit_log
        WHERE processed = false
          AND (retry_count IS NULL OR retry_count < ?)
        """
            + (txId != null ? " AND tx_id = ? " : " ")
            + """
            GROUP BY tx_id, related_model, related_id, event_type
            ORDER BY MIN(created_on)
            """;

    Session session = JPA.em().unwrap(Session.class);
    List<AuditWorkGroup> result = new ArrayList<>();

    session.doWork(
        conn -> {
          try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, MAX_RETRY);
            if (txId != null) {
              ps.setString(2, txId);
            }
            // Fetch small chunks
            ps.setMaxRows(BATCH_SIZE);

            try (ResultSet rs = ps.executeQuery()) {
              while (rs.next()) {
                result.add(
                    new AuditWorkGroup(
                        rs.getString("tx_id"),
                        rs.getString("related_model"),
                        rs.getLong("related_id"),
                        AuditEventType.valueOf(rs.getString("event_type"))));
              }
            }
          }
        });
    return result;
  }

  private static class AuditWorkGroup {
    private final String txId;
    private final String relatedModel;
    private final Long relatedId;
    private final AuditEventType eventType;

    public AuditWorkGroup(
        String txId, String relatedModel, Long relatedId, AuditEventType eventType) {
      this.txId = txId;
      this.relatedModel = relatedModel;
      this.relatedId = relatedId;
      this.eventType = eventType;
    }

    public String getTxId() {
      return txId;
    }

    public String getRelatedModel() {
      return relatedModel;
    }

    public Long getRelatedId() {
      return relatedId;
    }

    public AuditEventType getEventType() {
      return eventType;
    }

    /** Retrieves a list of audit logs for a specified audit work group. */
    private List<AuditLog> fetchLogs() {
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

    public String getDescription() {
      return String.format("%s#%d (%s)", relatedModel, relatedId, eventType);
    }

    @Override
    public String toString() {
      return getDescription() + " [Tx: " + txId + "]";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      AuditWorkGroup that = (AuditWorkGroup) o;
      return Objects.equals(txId, that.txId)
          && Objects.equals(relatedModel, that.relatedModel)
          && Objects.equals(relatedId, that.relatedId)
          && eventType == that.eventType;
    }

    @Override
    public int hashCode() {
      return Objects.hash(txId, relatedModel, relatedId, eventType);
    }
  }
}
