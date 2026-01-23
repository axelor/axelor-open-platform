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
import com.axelor.db.internal.DBHelper;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.service.MailMessageTrackingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.hibernate.Session;
import org.postgresql.util.PSQLException;
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
  private static final int BATCH_SIZE =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_BATCH_SIZE, 100);
  private static final int MAX_RETRY =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_LOGS_MAX_RETRY, 3);

  private final MailMessageTrackingService service;
  private final ObjectMapper objectMapper;
  private BooleanSupplier keepRunningSupplier;

  // Throttling constants
  private static final long BATCH_DELAY_MS =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_BATCH_DELAY, 5);
  private static final long BUSY_BACKOFF_INTERVAL =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_BUSY_BACKOFF_INTERVAL, 200);
  private static final long BUSY_BACKOFF_MAX_RETRIES =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_BUSY_BACKOFF_MAX_RETRIES, 3);
  private static final long ACTIVITY_WINDOW_MS =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_ACTIVITY_WINDOW, 200);

  // The last time activity was signaled
  private static volatile long lastActivityTime = 0;

  public AuditProcessor() {
    this.service = Beans.get(MailMessageTrackingService.class);
    this.objectMapper = Beans.get(ObjectMapper.class);
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
    List<String> candidateTxIds = fetchCandidateTxIds(BATCH_SIZE);

    if (candidateTxIds.isEmpty()) {
      return;
    }

    log.info("Recovering audit logs...");

    for (String txId : candidateTxIds) {
      if (isShutdownRequest()) {
        break;
      }
      // Delegate to the specific processor
      process(txId);
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

  private boolean isShutdownRequest() {
    return (keepRunningSupplier != null && !keepRunningSupplier.getAsBoolean())
        || Thread.currentThread().isInterrupted();
  }

  /** Core processing loop that fetches and processes audit logs in batches. */
  private void processPendingWork(String txId) {
    int currentOffset = 0;
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
      int finalCurrentOffset = currentOffset;
      BatchResult result;
      try {
        result = JPA.callInTransaction(() -> processBatch(txId, finalCurrentOffset));
      } catch (Exception e) {
        if (isLockingException(e)) {
          break;
        }
        log.error("Unexpected error processing txId: {}", txId, e);
        break;
      }
      totalProcessed += result.succeeded();
      totalFailed += result.failed();

      // Move offset
      currentOffset += result.failed();

      // Everything processed, exit
      if ((result.succeeded() + result.failed()) < BATCH_SIZE) {
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
   * Determines whether the given throwable or any of its causes represent a locking-related
   * exception. Specifically, it checks for instances of {@code
   * jakarta.persistence.PessimisticLockException}, {@code org.hibernate.PessimisticLockException},
   * or {@code org.hibernate.exception.LockAcquisitionException} or {@code
   * org.postgresql.util.PSQLException} with 55P03 SQL state.
   *
   * @param e the throwable to examine; may be null
   * @return {@code true} if the throwable or any of its causes is a locking-related exception,
   *     {@code false} otherwise
   */
  private boolean isLockingException(Throwable e) {
    if (e == null) return false;
    if (e instanceof PSQLException psqlException && "55P03".equals(psqlException.getSQLState()))
      return true;
    if (e instanceof jakarta.persistence.PessimisticLockException
        || e instanceof org.hibernate.PessimisticLockException
        || e instanceof org.hibernate.exception.LockAcquisitionException) return true;
    if (e.getCause() != null && e.getCause() != e) return isLockingException(e.getCause());
    return false;
  }

  protected BatchResult processBatch(String txId, int offset) {
    // compute audit work group
    List<AuditWorkGroup> batch = fetchNextBatch(txId, offset);
    if (batch.isEmpty()) {
      return new BatchResult(0, 0);
    }

    // fetch associated audit logs
    fetchLogsForBatch(batch);

    var processedAuditWorkGroups = new ArrayList<AuditWorkGroup>();
    int failedInBatch = 0;

    // process
    for (AuditWorkGroup auditWorkGroup : batch) {
      try {
        process(auditWorkGroup);
        processedAuditWorkGroups.add(auditWorkGroup);
      } catch (Exception e) {
        failedInBatch++;
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

    return new BatchResult(processedAuditWorkGroups.size(), failedInBatch);
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

  /**
   * Handles errors that occur while processing audit logs for a given audit work group.
   *
   * <p>Updates the retry count, marks the appropriate logs as processed if maximum retries are
   * exceeded, and updates the associated audit log records with error details.
   */
  private void handleError(AuditWorkGroup group, Exception e) {
    log.error("Failed to process audit logs for {}", group, e);

    var message = e.getMessage();
    if (message != null && message.length() > 1000) {
      message = message.substring(0, 1000);
    }

    AuditLog auditLog = group.getFirstAuditLog();
    boolean processed = false;
    int maxRetry =
        ((auditLog != null && auditLog.getRetryCount() != null) ? auditLog.getRetryCount() : 0) + 1;
    if (maxRetry >= MAX_RETRY) {
      log.error("Max retries exceeded for audit logs group {}", group);
      processed = true;
    }

    JPA.em()
        .createQuery(
            """
                  UPDATE AuditLog SET processed = :processed, retryCount = :retry, errorMessage = :message
                  WHERE processed = false AND txId = :txId AND relatedModel = :relatedModel AND relatedId = :relatedId AND eventType = :eventType
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

  /**
   * Deletes audit log records associated with the given list of processed audit work groups. This
   * method directly executes a batch deletion query for each group in the provided list.
   */
  private void deleteProcessedGroups(List<AuditWorkGroup> groups) {
    String sql =
        "DELETE FROM audit_log WHERE processed=false AND tx_id=? AND related_model=? AND related_id=? AND event_type=?";
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

    var idsToFetch = new HashSet<>();
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

  /**
   * Retrieves the next batch of unprocessed audit logs from the database according to specific
   * criteria such as transaction ID and offset. The logs are grouped into {@code AuditWorkGroup}
   * objects for further processing.
   *
   * @param txId the ID of the transaction to filter the audit logs; if null, logs for all
   *     transactions will be fetched
   * @param offset the offset from which to start fetching the audit logs; used for pagination
   * @return a list of {@code AuditWorkGroup} objects representing the grouped unprocessed audit
   *     logs
   */
  private List<AuditWorkGroup> fetchNextBatch(String txId, int offset) {
    log.trace("Fetching next batch of audit logs with txId: {}, offset: {}", txId, offset);

    String sqlTemplate =
        """
            WITH locked_rows AS (
                 SELECT id, tx_id, related_model, related_id, event_type, created_on
                 FROM audit_log
                 WHERE processed = false
                   AND tx_id = ?
                 %s
            )
            SELECT tx_id, related_model, related_id, event_type, MIN(id) as min_id, MAX(id) as max_id
            FROM locked_rows
            GROUP BY tx_id, related_model, related_id, event_type
            ORDER BY MIN(created_on)
            LIMIT ? OFFSET ?
            """;

    String sql = sqlTemplate.formatted(DBHelper.isPostgreSQL() ? "FOR UPDATE NOWAIT" : "");

    Session session = JPA.em().unwrap(Session.class);
    List<AuditWorkGroup> result = new ArrayList<>();

    session.doWork(
        conn -> {
          try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, txId);
            // Fetch small chunks
            ps.setInt(idx++, BATCH_SIZE);
            ps.setInt(idx++, offset);

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

  /**
   * Retrieves a list of candidate transaction IDs from the audit log table that have not been
   * processed.
   *
   * @param limit the maximum number of transaction IDs to fetch from the database
   * @return a list of unprocessed transaction IDs, ordered by the earliest creation time
   */
  private List<String> fetchCandidateTxIds(int limit) {
    String sql =
        """
          SELECT tx_id
          FROM audit_log
          WHERE processed = false
          GROUP BY tx_id
          ORDER BY MIN(created_on)
          LIMIT ?
          """;

    List<String> result = new ArrayList<>();
    JPA.em()
        .unwrap(Session.class)
        .doWork(
            conn -> {
              try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                  while (rs.next()) {
                    result.add(rs.getString(1));
                  }
                }
              }
            });
    return result;
  }

  protected record BatchResult(int succeeded, int failed) {}

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
