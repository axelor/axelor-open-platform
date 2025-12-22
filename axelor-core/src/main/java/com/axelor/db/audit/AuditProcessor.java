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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

      // eliminate round-trips to the database
      fetchLogsForBatch(batch);

      var processedAuditWorkGroups = new ArrayList<AuditWorkGroup>();

      for (AuditWorkGroup auditWorkGroup : batch) {
        try {
          process(auditWorkGroup);
          processedAuditWorkGroups.add(auditWorkGroup);
          totalProcessed++;
        } catch (Exception e) {
          totalFailed++;
          handleError(auditWorkGroup, e);
        }
      }

      // Flush pending changes (e.g., MailMessage inserts, error updates)
      JPA.flush();

      // Bulk delete successfully processed AuditLogs
      if (!processedAuditWorkGroups.isEmpty()) {
        deleteProcessedGroups(processedAuditWorkGroups);
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

  private void process(AuditWorkGroup group) throws Exception {
    if (group.getLogs().isEmpty()) {
      return;
    }

    log.trace("Processing audit logs for {} ", group);

    // Consolidate ALL changes from all audit logs in this transaction
    var firstLog = group.getFirstAuditLog();
    var lastLog = group.getLastAuditLog();
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
  }

  private void handleError(AuditWorkGroup group, Exception e) {
    log.error("Failed to process audit logs for {}", group, e);

    var message = e.getMessage();
    if (message != null && message.length() > 1000) {
      message = message.substring(0, 1000);
    }

    AuditLog auditLog = group.getFirstAuditLog();
    boolean processed = false;
    int maxRetry = auditLog.getRetryCount() + 1;
    if (maxRetry >= MAX_RETRY) {
      log.error("Max retries exceeded for audit logs group {}", group);
      processed = true;
    }

    JPA.em()
        .createQuery(
            """
                  UPDATE AuditLog SET processed = :processed, retryCount = :retry, errorMessage = :message
                  WHERE txId = :txId AND relatedModel = :relatedModel AND relatedId = :relatedId AND eventType = :eventType
              """)
        .setParameter("processed", processed)
        .setParameter("retry", maxRetry)
        .setParameter("message", message)
        .setParameter("txId", group.getTxId())
        .setParameter("relatedModel", group.getRelatedModel())
        .setParameter("relatedId", group.getRelatedId())
        .setParameter("eventType", group.getEventType())
        .executeUpdate();
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

  private void deleteProcessedGroups(List<AuditWorkGroup> groups) {
    String sql =
        "DELETE FROM audit_log WHERE tx_id=? AND related_model=? AND related_id=? AND event_type=?";
    JPA.em()
        .unwrap(Session.class)
        .doWork(
            conn -> {
              try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (AuditWorkGroup g : groups) {
                  ps.setString(1, g.getTxId());
                  ps.setString(2, g.getRelatedModel());
                  ps.setObject(3, g.getRelatedId());
                  ps.setString(4, g.getEventType().name());
                  ps.addBatch();
                }
                ps.executeBatch();
              }
            });
  }

  /** Fetches all AuditLogs for the entire batch of groups in a single query. */
  private void fetchLogsForBatch(List<AuditWorkGroup> batch) {
    if (batch.isEmpty()) {
      return;
    }

    var idsToFetch = new ArrayList<>();
    // fast lookup map
    Map<AuditWorkGroup, AuditWorkGroup> groupMap = new HashMap<>();

    for (var g : batch) {
      idsToFetch.add(g.getFirstAuditLogId());
      idsToFetch.add(g.getLastAuditLogId());
      groupMap.put(g, g);
    }

    List<AuditLog> logs =
        JPA.em()
            .createQuery("SELECT a FROM AuditLog a WHERE a.id IN :ids", AuditLog.class)
            .setParameter("ids", idsToFetch)
            .getResultList();

    for (AuditLog log : logs) {
      AuditWorkGroup tmpWorkGroup =
          new AuditWorkGroup(
              log.getTxId(), log.getRelatedModel(), log.getRelatedId(), log.getEventType());

      AuditWorkGroup targetGroup = groupMap.get(tmpWorkGroup);
      if (targetGroup != null) {
        targetGroup.addAuditLog(log);
      }
    }
  }

  private List<AuditWorkGroup> fetchNextBatch(String txId) {
    String sql =
        """
        SELECT tx_id, related_model, related_id, event_type, MIN(id) as min_id, MAX(id) as max_id
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
                        AuditEventType.valueOf(rs.getString("event_type")),
                        rs.getLong("min_id"),
                        rs.getLong("max_id")));
              }
            }
          }
        });
    return result;
  }

  private static class AuditWorkGroup {

    String txId;
    String relatedModel;
    Long relatedId;
    Long firstAuditLogId;
    Long lastAuditLogId;
    AuditEventType eventType;

    List<AuditLog> logs = new ArrayList<>();

    public AuditWorkGroup(
        String txId, String relatedModel, Long relatedId, AuditEventType eventType) {
      this.eventType = eventType;
      this.relatedId = relatedId;
      this.relatedModel = relatedModel;
      this.txId = txId;
    }

    public AuditWorkGroup(
        String txId,
        String relatedModel,
        Long relatedId,
        AuditEventType eventType,
        Long firstAuditLogId,
        Long lastAuditLogId) {
      this.txId = txId;
      this.relatedModel = relatedModel;
      this.relatedId = relatedId;
      this.firstAuditLogId = firstAuditLogId;
      this.lastAuditLogId = lastAuditLogId;
      this.eventType = eventType;
    }

    public AuditEventType getEventType() {
      return eventType;
    }

    public Long getRelatedId() {
      return relatedId;
    }

    public String getRelatedModel() {
      return relatedModel;
    }

    public String getTxId() {
      return txId;
    }

    public Long getFirstAuditLogId() {
      return firstAuditLogId;
    }

    public Long getLastAuditLogId() {
      return lastAuditLogId;
    }

    public void addAuditLog(AuditLog log) {
      this.logs.add(log);
    }

    public List<AuditLog> getLogs() {
      return logs;
    }

    public AuditLog getFirstAuditLog() {
      return logs.stream().min(Comparator.comparingLong(AuditLog::getId)).orElse(null);
    }

    public AuditLog getLastAuditLog() {
      return logs.stream().max(Comparator.comparingLong(AuditLog::getId)).orElse(null);
    }

    @Override
    public String toString() {
      return String.format("%s#%d (%s) [Tx: %s]", relatedModel, relatedId, eventType, txId);
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
