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
import com.axelor.db.mapper.Adapter;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.service.MailMessageTrackingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
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
import org.hibernate.jdbc.Work;
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
  private final int BATCH_SIZE =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_PROCESSOR_BATCH_SIZE, 100);
  private final int MAX_RETRY =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_LOGS_MAX_RETRY, 3);

  private final MailMessageTrackingService service;
  private final ObjectMapper objectMapper;
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
      long afterAuditLogId = lastAuditLogId;
      BatchResult result;
      try {
        result = JPA.callInTransaction(() -> processBatch(txId, afterAuditLogId));
      } catch (Exception e) {
        // Not caused by a specific group (e.g. deleting processed logs or commit)
        if (!isAuditLogLocked(e)) {
          log.error("Unexpected error processing txId: {}", txId, e);
        }
        break;
      } finally {
        // Discard any entity left by the batch, notably when it was rolled back
        JPA.clear();
      }

      if (result.failedGroup() == null) {
        totalProcessed += result.processedGroups().size();

        // Move cursor past this batch
        lastAuditLogId = result.lastAuditLogId();

        // Everything processed, exit
        if (result.fetched() < BATCH_SIZE) {
          break;
        }
      } else {
        // update the failed group
        totalFailed++;
        recordError(result.failedGroup(), result.failure());

        // Groups processed before the failure were rolled back too, process them again one by
        // one, so that another failure doesn't roll them back again
        boolean locked = false;
        for (AuditWorkGroup group : result.processedGroups()) {
          try {
            if (JPA.callInTransaction(() -> processGroup(group))) {
              totalProcessed++;
            }
          } catch (Exception e) {
            if (isAuditLogLocked(e)) {
              locked = true;
              break;
            }
            totalFailed++;
            recordError(group, e);
          } finally {
            JPA.clear();
          }
        }
        if (locked) {
          break;
        }

        // Move cursor past the failed group, the next batch starts with the groups after it
        lastAuditLogId = result.failedGroup().getFirstAuditLogId();
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

  /**
   * Determines whether the given throwable or any of its causes is an {@link
   * AuditLogLockedException}, meaning the pending audit logs are being processed by another worker.
   */
  private boolean isAuditLogLocked(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
      if (t instanceof AuditLogLockedException) {
        return true;
      }
    }
    return false;
  }

  /**
   * Executes a JDBC work locking audit logs. A locking failure is thrown as an {@link
   * AuditLogLockedException}, to tell it apart from locking failures on other tables.
   */
  private void doAuditLogLockingWork(Work work) {
    try {
      JPA.em().unwrap(Session.class).doWork(work);
    } catch (RuntimeException e) {
      if (isLockingException(e)) {
        throw new AuditLogLockedException(e);
      }
      throw e;
    }
  }

  /**
   * Processes the next batch of groups in the current transaction.
   *
   * <p>Processing stops at the first failing group, and the transaction is marked for rollback: a
   * failing group must not leave partial changes, and a database error may have made the
   * transaction unusable. The failing group and the groups processed before it are returned, so the
   * caller can record the error and process these groups again once the batch is rolled back.
   */
  protected BatchResult processBatch(String txId, long afterAuditLogId) {
    // compute audit work group
    List<AuditWorkGroup> batch = fetchNextBatch(txId, afterAuditLogId);
    if (batch.isEmpty()) {
      return new BatchResult(0, List.of(), null, null, afterAuditLogId);
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
        return new BatchResult(batch.size(), processedGroups, auditWorkGroup, e, lastAuditLogId);
      }
      processedGroups.add(auditWorkGroup);
    }

    // Bulk delete successfully processed AuditLogs
    deleteProcessedGroups(batch);

    checkNotRollbackOnly();

    return new BatchResult(batch.size(), processedGroups, null, null, lastAuditLogId);
  }

  /**
   * Processes a single group in the current transaction.
   *
   * @return {@code false} if the group has already been processed meanwhile
   */
  private boolean processGroup(AuditWorkGroup group) {
    if (!lockGroup(group)) {
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

  /** Records the error of a failed group in a new transaction. */
  private void recordError(AuditWorkGroup group, Exception e) {
    try {
      JPA.runInTransaction(() -> handleError(group, e));
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

    var message = e.getMessage();
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
   * Locks the pending audit logs of the given group.
   *
   * @return {@code false} if the group has no pending audit logs anymore
   */
  private boolean lockGroup(AuditWorkGroup group) {
    String sql =
        """
            SELECT id FROM audit_log
            WHERE processed = false AND tx_id = ? AND related_model = ? AND related_id = ? AND event_type = ?
            %s
            """
            .formatted(DBHelper.isPostgreSQL() ? "FOR UPDATE NOWAIT" : "");

    var found = new boolean[1];
    doAuditLogLockingWork(
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

    String sqlTemplate =
        """
            WITH locked_rows AS (
                 SELECT id, tx_id, related_model, related_id, event_type, retry_count, created_on
                 FROM audit_log
                 WHERE processed = false
                   AND tx_id = ?
                 %s
            )
            SELECT tx_id, related_model, related_id, event_type, MIN(id) as min_id, MAX(id) as max_id,
                   MAX(COALESCE(retry_count, 0)) as retry_count
            FROM locked_rows
            GROUP BY tx_id, related_model, related_id, event_type
            HAVING MIN(id) > ?
            ORDER BY MIN(id)
            LIMIT ?
            """;

    String sql = sqlTemplate.formatted(DBHelper.isPostgreSQL() ? "FOR UPDATE NOWAIT" : "");

    List<AuditWorkGroup> result = new ArrayList<>();

    doAuditLogLockingWork(
        conn -> {
          try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, txId);
            ps.setLong(idx++, afterAuditLogId);
            // Fetch small chunks
            ps.setInt(idx, BATCH_SIZE);

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

  /**
   * Result of a batch.
   *
   * @param fetched the number of groups fetched
   * @param processedGroups the groups processed successfully; when the batch failed, the groups
   *     processed before the failure, rolled back with the batch
   * @param failedGroup the group which failed, or null if the batch succeeded
   * @param failure the failure of the failed group
   * @param lastAuditLogId the cursor after this batch
   */
  protected record BatchResult(
      int fetched,
      List<AuditWorkGroup> processedGroups,
      AuditWorkGroup failedGroup,
      Exception failure,
      long lastAuditLogId) {}

  /** Thrown when the pending audit logs are locked by another worker. */
  private static class AuditLogLockedException extends RuntimeException {
    AuditLogLockedException(Throwable cause) {
      super(cause);
    }
  }

  private static class AuditWorkGroup {

    String txId;
    String relatedModel;
    Long relatedId;
    Long firstAuditLogId;
    Long lastAuditLogId;
    AuditEventType eventType;
    int retryCount;

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
        Long lastAuditLogId,
        int retryCount) {
      this.txId = txId;
      this.relatedModel = relatedModel;
      this.relatedId = relatedId;
      this.firstAuditLogId = firstAuditLogId;
      this.lastAuditLogId = lastAuditLogId;
      this.eventType = eventType;
      this.retryCount = retryCount;
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

    public int getRetryCount() {
      return retryCount;
    }

    public Long getLastAuditLogId() {
      return lastAuditLogId;
    }

    public void addAuditLog(AuditLog log) {
      this.logs.add(log);
    }

    public void clearAuditLogs() {
      this.logs.clear();
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
