/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.audit.db.AuditEventType;
import com.axelor.audit.db.AuditLog;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.audit.state.AuditState;
import com.axelor.db.audit.state.EntityState;
import com.axelor.db.internal.DBHelper;
import com.axelor.db.mapper.Adapter;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.mail.service.MailMessageTrackingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes the pending audit logs of a transaction, one batch of groups at a time.
 *
 * <p>A batch is processed in a single database transaction. If a group fails, the batch is rolled
 * back, the error of the failing group is recorded for a later retry, and the groups processed
 * before it are processed again, each in its own transaction.
 *
 * <p>On PostgreSQL, an advisory lock on the transaction ID ensures that only one worker at a time
 * processes the audit logs of a transaction.
 */
class AuditBatchProcessor {

  private static final Logger log = LoggerFactory.getLogger(AuditBatchProcessor.class);

  // First key of the advisory locks taken on transaction IDs. The two-key form doesn't overlap with
  // single-key advisory locks, like the leader election one.
  static final int ADVISORY_LOCK_CLASS_ID = 0x41554454; // "AUDT"

  private final int MAX_RETRY =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_LOGS_MAX_RETRY, 3);

  private final int batchSize;
  private final MailMessageTrackingService service;
  private final ObjectMapper objectMapper;

  AuditBatchProcessor(int batchSize) {
    this.batchSize = batchSize;
    this.service = Beans.get(MailMessageTrackingService.class);
    this.objectMapper = Beans.get(ObjectMapper.class);
  }

  /**
   * Processes the next batch of groups of the given transaction.
   *
   * <p>Runs its own database transactions. A failing group doesn't prevent the other groups of the
   * batch to be processed, and its error is recorded for a later retry.
   *
   * @param txId the ID of the transaction whose audit logs are processed
   * @param afterAuditLogId only groups whose first audit log ID is greater than this are processed;
   *     0 to start from the first group
   * @return the summary of the batch
   */
  BatchResult processNextBatch(String txId, long afterAuditLogId) {
    BatchAttempt attempt;
    try {
      attempt = JPA.callInTransaction(() -> processBatch(txId, afterAuditLogId));
    } finally {
      // Discard any entity left by the batch, notably when it was rolled back
      JPA.clear();
    }

    if (attempt.failedGroup() == null) {
      return new BatchResult(
          attempt.processedGroups().size(),
          0,
          attempt.lastAuditLogId(),
          attempt.fetched() >= batchSize);
    }

    // The batch was rolled back, record the error of the failed group
    int processed = 0;
    int failed = 1;
    recordError(attempt.failedGroup(), attempt.failure());

    // Groups processed before the failure were rolled back too, process them again one by one, so
    // that another failure doesn't roll them back again
    for (AuditWorkGroup group : attempt.processedGroups()) {
      try {
        if (JPA.callInTransaction(() -> processGroup(group))) {
          processed++;
        }
      } catch (Exception e) {
        failed++;
        recordError(group, e);
      } finally {
        JPA.clear();
      }
    }

    // Move cursor past the failed group, the next batch starts with the groups after it
    return new BatchResult(processed, failed, attempt.failedGroup().getFirstAuditLogId(), true);
  }

  /**
   * Takes an advisory lock on the given transaction ID, held until the end of the current database
   * transaction, so that only one worker at a time processes the audit logs of a transaction.
   *
   * <p>Only on PostgreSQL; on other databases, no lock is taken.
   *
   * @return {@code false} if the lock is held by another worker
   */
  private boolean lockTransaction(String txId) {
    if (!DBHelper.isPostgreSQL()) {
      return true;
    }
    var locked = new boolean[1];
    JPA.em()
        .unwrap(Session.class)
        .doWork(
            conn -> {
              try (PreparedStatement ps =
                  conn.prepareStatement("SELECT pg_try_advisory_xact_lock(?, hashtext(?))")) {
                ps.setInt(1, ADVISORY_LOCK_CLASS_ID);
                ps.setString(2, txId);
                try (ResultSet rs = ps.executeQuery()) {
                  locked[0] = rs.next() && rs.getBoolean(1);
                }
              }
            });
    if (!locked[0]) {
      log.debug("Audit logs of transaction {} are processed by another worker", txId);
    }
    return locked[0];
  }

  /**
   * Processes the next batch of groups in the current transaction.
   *
   * <p>Processing stops at the first failing group, and the transaction is marked for rollback: a
   * failing group must not leave partial changes, and a database error may have made the
   * transaction unusable. The failing group and the groups processed before it are returned, so
   * their errors can be recorded and these groups processed again once the batch is rolled back.
   *
   * <p>If another worker is processing the same transaction, nothing is fetched.
   */
  private BatchAttempt processBatch(String txId, long afterAuditLogId) {
    if (!lockTransaction(txId)) {
      return new BatchAttempt(0, List.of(), null, null, afterAuditLogId);
    }

    // compute audit work group
    List<AuditWorkGroup> batch = fetchNextBatch(txId, afterAuditLogId);
    if (batch.isEmpty()) {
      return new BatchAttempt(0, List.of(), null, null, afterAuditLogId);
    }
    long lastAuditLogId = batch.getLast().getFirstAuditLogId();

    // fetch associated audit logs
    fetchLogsForBatch(batch);

    // process
    var processedGroups = new ArrayList<AuditWorkGroup>();
    for (AuditWorkGroup auditWorkGroup : batch) {
      try {
        process(auditWorkGroup);
        // Flush now, so that a database error is reported on the group causing it
        JPA.flush();
        checkNotRollbackOnly();
      } catch (Exception e) {
        JPA.em().getTransaction().setRollbackOnly();
        return new BatchAttempt(batch.size(), processedGroups, auditWorkGroup, e, lastAuditLogId);
      }
      processedGroups.add(auditWorkGroup);
    }

    // Bulk delete successfully processed AuditLogs
    deleteProcessedGroups(batch);

    checkNotRollbackOnly();

    return new BatchAttempt(batch.size(), processedGroups, null, null, lastAuditLogId);
  }

  /**
   * Processes a single group in the current transaction.
   *
   * @return {@code false} if the group has already been processed meanwhile, or if another worker
   *     is processing its transaction
   */
  private boolean processGroup(AuditWorkGroup group) {
    if (!lockTransaction(group.getTxId()) || !hasPendingLogs(group)) {
      return false;
    }
    // Reload the logs, the ones loaded by a previous transaction are detached
    group.clearAuditLogs();
    fetchLogsForBatch(List.of(group));
    process(group);
    JPA.flush();
    deleteProcessedGroups(List.of(group));
    checkNotRollbackOnly();
    return true;
  }

  /**
   * Records the error of a failed group in a new transaction. Skipped if another worker is
   * processing its transaction, which will retry the group.
   */
  private void recordError(AuditWorkGroup group, Exception e) {
    try {
      JPA.runInTransaction(
          () -> {
            if (lockTransaction(group.getTxId())) {
              handleError(group, e);
            }
          });
    } catch (Exception ex) {
      log.error("Failed to record error for audit logs group {}", group, ex);
    }
  }

  /**
   * Fails if the current transaction has been marked for rollback, for example when an exception
   * was swallowed after a failed database operation. Otherwise, the transaction would be silently
   * rolled back while the work is reported as done.
   */
  private void checkNotRollbackOnly() {
    if (JPA.em().getTransaction().getRollbackOnly()) {
      throw new IllegalStateException("Transaction was marked for rollback only");
    }
  }

  private void process(AuditWorkGroup group) {
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
    Class<? extends Model> entityClass;
    try {
      entityClass = Class.forName(group.getRelatedModel()).asSubclass(Model.class);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Unknown model: " + group.getRelatedModel(), e);
    }
    var entity = JPA.findReferenceById(entityClass, group.getRelatedId());

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

    var message = e.getMessage() != null ? e.getMessage() : e.toString();
    if (message != null && message.length() > 1000) {
      message = message.substring(0, 1000);
    }

    boolean processed = false;
    int maxRetry = group.getRetryCount() + 1;
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
    var parsedValues = new HashMap<String, Object>();
    for (var entry : values.entrySet()) {
      var name = entry.getKey();
      var value = entry.getValue();
      var prop = mapper.getProperty(name);
      if (prop == null || prop.isReference()) {
        parsedValues.put(name, value);
      } else {
        parsedValues.put(
            name, Adapter.adapt(value, prop.getJavaType(), prop.getGenericType(), null));
      }
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
    if (StringUtils.isBlank(json)) {
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
   * Checks whether the given group still has pending audit logs.
   *
   * @return {@code false} if the group has no pending audit logs anymore
   */
  private boolean hasPendingLogs(AuditWorkGroup group) {
    String sql =
        """
            SELECT id FROM audit_log
            WHERE processed = false AND tx_id = ? AND related_model = ? AND related_id = ? AND event_type = ?
            """;

    var found = new boolean[1];
    JPA.em()
        .unwrap(Session.class)
        .doWork(
            conn -> {
              try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, group.getTxId());
                ps.setString(2, group.getRelatedModel());
                ps.setObject(3, group.getRelatedId());
                ps.setString(4, group.getEventType().name());
                try (ResultSet rs = ps.executeQuery()) {
                  found[0] = rs.next();
                }
              }
            });
    return found[0];
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

    Set<Long> idsToFetch = new HashSet<>();
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
   * Retrieves the next batch of unprocessed audit logs of the given transaction, using keyset
   * pagination on the first audit log ID of each group. The logs are grouped into {@code
   * AuditWorkGroup} objects for further processing.
   *
   * @param txId the ID of the transaction whose unprocessed audit logs are fetched; must not be
   *     null
   * @param afterAuditLogId only groups whose first audit log ID is greater than this are fetched; 0
   *     to start from the first group
   * @return a list of {@code AuditWorkGroup} objects representing the grouped unprocessed audit
   *     logs
   */
  private List<AuditWorkGroup> fetchNextBatch(String txId, long afterAuditLogId) {
    log.trace(
        "Fetching next batch of audit logs with txId: {}, after id: {}", txId, afterAuditLogId);

    String sql =
        """
            SELECT tx_id, related_model, related_id, event_type, MIN(id) as min_id, MAX(id) as max_id,
                   MAX(COALESCE(retry_count, 0)) as retry_count
            FROM audit_log
            WHERE processed = false
              AND tx_id = ?
            GROUP BY tx_id, related_model, related_id, event_type
            HAVING MIN(id) > ?
            ORDER BY MIN(id)
            LIMIT ?
            """;

    Session session = JPA.em().unwrap(Session.class);
    List<AuditWorkGroup> result = new ArrayList<>();

    session.doWork(
        conn -> {
          try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, txId);
            ps.setLong(idx++, afterAuditLogId);
            // Fetch small chunks
            ps.setInt(idx, batchSize);

            try (ResultSet rs = ps.executeQuery()) {
              while (rs.next()) {
                result.add(
                    new AuditWorkGroup(
                        rs.getString("tx_id"),
                        rs.getString("related_model"),
                        rs.getLong("related_id"),
                        AuditEventType.valueOf(rs.getString("event_type")),
                        rs.getLong("min_id"),
                        rs.getLong("max_id"),
                        rs.getInt("retry_count")));
              }
            }
          }
        });
    return result;
  }

  /**
   * Result of a batch processed in a single database transaction.
   *
   * @param fetched the number of groups fetched
   * @param processedGroups the groups processed successfully; when the batch failed, the groups
   *     processed before the failure, rolled back with the batch
   * @param failedGroup the group which failed, or null if the batch succeeded
   * @param failure the failure of the failed group
   * @param lastAuditLogId the cursor after this batch
   */
  private record BatchAttempt(
      int fetched,
      List<AuditWorkGroup> processedGroups,
      AuditWorkGroup failedGroup,
      Exception failure,
      long lastAuditLogId) {}
}
