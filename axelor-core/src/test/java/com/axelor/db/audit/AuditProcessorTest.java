/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.audit.db.AuditEventType;
import com.axelor.audit.db.AuditLog;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.db.audit.state.AuditState;
import com.axelor.db.internal.DBHelper;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.service.MailMessageTrackingService;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.AuditCheck;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests {@link AuditProcessor} batch processing and error handling.
 *
 * <p>Pending audit logs span several batches, and some groups are made to fail in different ways: a
 * failing group must not prevent the other groups to be processed, and its retry count must not be
 * lost when the batch is rolled back.
 */
@GuiceModules(AuditProcessorTest.ProcessorTestModule.class)
class AuditProcessorTest extends BaseAuditTest {

  // Small batch size so tests span several batches
  private static final int BATCH_SIZE = 5;
  private static final int MAX_RETRY =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_LOGS_MAX_RETRY, 3);

  // Several full batches plus a partial one
  private static final int GROUP_COUNT = BATCH_SIZE * 2 + 3;

  // Entities with these name prefixes fail during processing, see FailingTrackingService
  private static final String POISON = "Poison";
  private static final String FLUSH_FAIL = "FlushFail";
  private static final String FLAKY = "Flaky";

  /** Ways to make a group fail during processing. */
  enum Failure {
    /** Error outside the database: the model class doesn't exist. */
    UNKNOWN_MODEL,
    /** Database error inside the tracking service: a failing query. */
    QUERY_ERROR,
    /** Database error when flushing the group changes: a duplicate login. */
    FLUSH_ERROR
  }

  public static class ProcessorTestModule extends AuditTestModule {
    @Override
    protected void configure() {
      super.configure();
      bind(MailMessageTrackingService.class).to(FailingTrackingService.class);
    }
  }

  /** Tracking service failing for entities with specific name prefixes. */
  public static class FailingTrackingService extends MailMessageTrackingService {

    // Processing attempts by entity ID
    static final Map<Long, Integer> attempts = new ConcurrentHashMap<>();

    @Override
    public void process(AuditState state, User user) {
      var name = state.getEntity() instanceof AuditCheck entity ? entity.getName() : null;
      if (name == null) {
        super.process(state, user);
        return;
      }

      var attempt = attempts.merge(state.getEntity().getId(), 1, Integer::sum);
      if (name.startsWith(POISON) || (name.startsWith(FLAKY) && attempt > 1)) {
        JPA.em().createNativeQuery("SELECT * FROM missing_table").getResultList();
      }

      super.process(state, user);

      if (name.startsWith(FLUSH_FAIL)) {
        // Duplicate login, only detected by the database when flushing
        var duplicate = new User();
        duplicate.setCode("admin");
        duplicate.setName("Duplicate");
        duplicate.setPassword("password");
        JPA.em().persist(duplicate);
      }
    }
  }

  @BeforeAll
  static void setBatchSize() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.AUDIT_PROCESSOR_BATCH_SIZE, String.valueOf(BATCH_SIZE));
  }

  @BeforeEach
  void cleanUp() {
    deleteAuditLogs();
    FailingTrackingService.attempts.clear();
  }

  // --- Pagination ---

  /** All groups of a transaction are processed, even when they span several batches. */
  @Test
  void shouldProcessAllGroupsOfTransactionAcrossBatches() throws Exception {
    var ids = asAdmin(this::createTrackedInOneTransaction);
    assertEquals(GROUP_COUNT, countPendingAuditLogs());

    processAuditLogs();

    assertEquals(0, countPendingAuditLogs());
    assertGroupsProcessedOnce(ids, List.of());
  }

  /** All pending transactions are recovered in a single run. */
  @Test
  void shouldRecoverAllTransactionsInSingleRun() throws Exception {
    var ids = new ArrayList<Long>();
    for (int i = 0; i < GROUP_COUNT; i++) {
      var name = "Tx " + i;
      ids.add(asAdmin(() -> createTracked(name)));
    }
    assertEquals(GROUP_COUNT, countPendingAuditLogs());

    processAuditLogs();

    assertEquals(0, countPendingAuditLogs());
    assertGroupsProcessedOnce(ids, List.of());
  }

  /**
   * Groups reaching their max retries leave the pending logs; the other groups must not be skipped.
   */
  @Test
  void shouldNotSkipGroupsWhenRetriesAreExhausted() throws Exception {
    var ids = asAdmin(this::createTrackedInOneTransaction);
    // Break one group out of two, so some of them fall in full batches
    var failingIds = new ArrayList<Long>();
    for (int i = 0; i < ids.size(); i += 2) {
      failingIds.add(ids.get(i));
      makeGroupFail(ids.get(i), Failure.UNKNOWN_MODEL);
      setRetryCount(ids.get(i), MAX_RETRY - 1);
    }

    processAuditLogs();

    for (Long failingId : failingIds) {
      var failingLog = auditLogOf(failingId);
      assertTrue(failingLog.getProcessed());
      assertEquals(MAX_RETRY, failingLog.getRetryCount());
    }
    assertEquals(0, countPendingAuditLogs(), "groups after the failing ones were skipped");
    assertGroupsProcessedOnce(ids, failingIds);
  }

  // --- Error handling ---

  /** The other groups are processed once, and the failing group gets its retry count increased. */
  @ParameterizedTest
  @EnumSource(Failure.class)
  void shouldProcessOtherGroupsWhenGroupFails(Failure failure) throws Exception {
    var ids = asAdmin(this::createTrackedInOneTransaction);
    var failingId = ids.get(2);
    makeGroupFail(failingId, failure);

    processAuditLogs();

    assertFailedOnce(failingId);
    assertEquals(1, countPendingAuditLogs(), "only the failing group should remain pending");
    assertGroupsProcessedOnce(ids, List.of(failingId));
  }

  /** The failing group is the first of its batch, with no group processed before it. */
  @ParameterizedTest
  @EnumSource(Failure.class)
  void shouldProcessOtherGroupsWhenFirstGroupOfBatchFails(Failure failure) throws Exception {
    var ids = asAdmin(this::createTrackedInOneTransaction);
    var failingId = ids.get(0);
    makeGroupFail(failingId, failure);

    processAuditLogs();

    assertFailedOnce(failingId);
    assertEquals(1, countPendingAuditLogs(), "only the failing group should remain pending");
    assertGroupsProcessedOnce(ids, List.of(failingId));
  }

  /** Several failing groups in the same batch: the other groups are processed exactly once. */
  @ParameterizedTest
  @EnumSource(Failure.class)
  void shouldProcessOtherGroupsOnceWhenSeveralGroupsFailInSameBatch(Failure failure)
      throws Exception {
    var ids = asAdmin(this::createTrackedInOneTransaction);
    var failingIds = List.of(ids.get(1), ids.get(3));
    failingIds.forEach(id -> makeGroupFail(id, failure));

    processAuditLogs();

    failingIds.forEach(this::assertFailedOnce);
    assertEquals(2, countPendingAuditLogs(), "only the failing groups should remain pending");
    assertGroupsProcessedOnce(ids, failingIds);
  }

  /** A failing group ends up discarded after its max retries. */
  @ParameterizedTest
  @EnumSource(Failure.class)
  void shouldDiscardFailingGroupAfterMaxRetries(Failure failure) throws Exception {
    var ids = asAdmin(this::createTrackedInOneTransaction);
    var failingId = ids.get(2);
    makeGroupFail(failingId, failure);

    for (int i = 0; i < MAX_RETRY; i++) {
      processAuditLogs();
    }

    var failingLog = auditLogOf(failingId);
    assertTrue(failingLog.getProcessed());
    assertEquals(MAX_RETRY, failingLog.getRetryCount());
    assertEquals(0, countPendingAuditLogs());
    assertGroupsProcessedOnce(ids, List.of(failingId));
  }

  /**
   * A group processed before a failing group is processed again once the batch is rolled back; if
   * it fails this time, its error is reported too.
   */
  @Test
  void shouldReportErrorOfGroupFailingWhenProcessedAgain() throws Exception {
    var ids = asAdmin(this::createTrackedInOneTransaction);
    var failingIds = List.of(ids.get(1), ids.get(2));
    rename(ids.get(1), FLAKY);
    rename(ids.get(2), POISON);

    processAuditLogs();

    failingIds.forEach(this::assertFailedOnce);
    assertEquals(2, countPendingAuditLogs(), "only the failing groups should remain pending");
    assertGroupsProcessedOnce(ids, failingIds);
  }

  // --- Concurrency ---

  /** A transaction locked by another worker is skipped, and processed once the lock is released. */
  @Test
  void shouldSkipTransactionLockedByAnotherWorker() throws Exception {
    Assumptions.assumeTrue(DBHelper.isPostgreSQL(), "advisory locks are only used on PostgreSQL");

    var ids = asAdmin(this::createTrackedInOneTransaction);
    var txId = Query.of(AuditLog.class).fetchOne().getTxId();

    // Another worker holds the lock of the transaction
    try (var conn = DBHelper.getConnection();
        var ps = conn.prepareStatement("SELECT pg_advisory_lock(?, hashtext(?))")) {
      ps.setInt(1, AuditProcessor.ADVISORY_LOCK_CLASS_ID);
      ps.setString(2, txId);
      ps.execute();

      processAuditLogs();

      assertEquals(GROUP_COUNT, countPendingAuditLogs(), "locked transaction was processed");
      for (Long id : ids) {
        assertEquals(0, countMessages(id));
        assertEquals(0, auditLogOf(id).getRetryCount());
      }
    }

    // Lock released with the connection
    processAuditLogs();

    assertEquals(0, countPendingAuditLogs());
    assertGroupsProcessedOnce(ids, List.of());
  }

  // --- Groups with several audit logs ---

  /**
   * A group with several audit logs is processed from the previous state of its first log to the
   * current state of its last log, and all its logs are removed.
   */
  @Test
  void shouldConsolidateGroupWithSeveralAuditLogs() throws Exception {
    var id = asAdmin(() -> createTracked("Multi D"));
    // Only keep the logs inserted below, not the one created with the entity
    deleteAuditLogs();
    insertUpdateLogs(id, List.of("Multi A", "Multi B", "Multi C", "Multi D"), 0, 0, 0);

    processAuditLogs();

    assertEquals(0, Query.of(AuditLog.class).count(), "all logs of the group should be removed");
    assertEquals(1, countMessages(id));
    var tracks = tracksOf(lastMessage(id));
    assertEquals("Multi D", tracks.get("name").get("value"));
    assertEquals("Multi A", tracks.get("name").get("oldValue"));
  }

  /**
   * A failing group with several audit logs is retried from the highest retry count of its logs,
   * and all its logs are updated.
   */
  @Test
  void shouldUpdateAllAuditLogsOfFailingGroup() throws Exception {
    var id = asAdmin(() -> createTracked("Multi"));
    // Only keep the logs inserted below, not the one created with the entity
    deleteAuditLogs();
    insertUpdateLogs(id, List.of("Multi A", "Multi B", "Multi C", "Multi D"), 0, MAX_RETRY - 1, 1);
    makeGroupFail(id, Failure.QUERY_ERROR);

    processAuditLogs();

    JPA.clear();
    var logs = Query.of(AuditLog.class).fetch();
    assertEquals(3, logs.size());
    for (var log : logs) {
      assertTrue(log.getProcessed());
      assertEquals(MAX_RETRY, log.getRetryCount());
      assertNotNull(log.getErrorMessage());
    }
  }

  // --- Helpers ---

  /**
   * Creates tracked entities in one transaction.
   *
   * @return the entity IDs in processing order, which follows the audit log IDs and not the
   *     creation order
   */
  private List<Long> createTrackedInOneTransaction() {
    JPA.runInTransaction(
        () -> {
          for (int i = 0; i < GROUP_COUNT; i++) {
            var entity = new AuditCheck();
            entity.setName("Entity " + i);
            JPA.em().persist(entity);
          }
        });
    JPA.clear();
    return Query.of(AuditLog.class).order("id").fetch().stream()
        .map(AuditLog::getRelatedId)
        .toList();
  }

  /** Makes the group of the given entity fail during processing. */
  private void makeGroupFail(Long entityId, Failure failure) {
    switch (failure) {
      case UNKNOWN_MODEL ->
          updateAuditLogs(entityId, "relatedModel", "com.axelor.test.db.UnknownModel");
      case QUERY_ERROR -> rename(entityId, POISON);
      case FLUSH_ERROR -> rename(entityId, FLUSH_FAIL);
    }
  }

  /**
   * Renames the entity with the given prefix, to make it fail during processing. A bulk update
   * doesn't create an audit log.
   */
  private void rename(Long entityId, String prefix) {
    JPA.runInTransaction(
        () ->
            JPA.em()
                .createQuery("UPDATE AuditCheck SET name = :name WHERE id = :id")
                .setParameter("name", prefix + " " + entityId)
                .setParameter("id", entityId)
                .executeUpdate());
  }

  private void setRetryCount(Long entityId, int retryCount) {
    updateAuditLogs(entityId, "retryCount", retryCount);
  }

  private void updateAuditLogs(Long entityId, String field, Object value) {
    JPA.runInTransaction(
        () ->
            JPA.em()
                .createQuery("UPDATE AuditLog SET " + field + " = :value WHERE relatedId = :id")
                .setParameter("value", value)
                .setParameter("id", entityId)
                .executeUpdate());
    JPA.clear();
  }

  /**
   * Inserts update logs of the given entity in the same transaction, one for each retry count. The
   * log at index {@code i} changes the name from {@code names[i]} to {@code names[i + 1]}.
   */
  private void insertUpdateLogs(Long entityId, List<String> names, int... retryCounts) {
    var txId = UUID.randomUUID().toString();
    var mapper = Beans.get(ObjectMapper.class);
    JPA.runInTransaction(
        () -> {
          for (int i = 0; i < retryCounts.length; i++) {
            var log = new AuditLog();
            log.setTxId(txId);
            log.setRelatedModel(AuditCheck.class.getName());
            log.setRelatedId(entityId);
            log.setEventType(AuditEventType.UPDATE);
            log.setProcessed(false);
            log.setRetryCount(retryCounts[i]);
            try {
              log.setPreviousState(mapper.writeValueAsString(Map.of("name", names.get(i))));
              log.setCurrentState(mapper.writeValueAsString(Map.of("name", names.get(i + 1))));
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
            JPA.em().persist(log);
          }
        });
  }

  private void deleteAuditLogs() {
    JPA.runInTransaction(() -> JPA.em().createQuery("DELETE FROM AuditLog").executeUpdate());
  }

  /** Asserts the group of the given entity failed once: still pending, with no message. */
  private void assertFailedOnce(Long entityId) {
    var log = auditLogOf(entityId);
    assertFalse(log.getProcessed());
    assertEquals(1, log.getRetryCount());
    assertNotNull(log.getErrorMessage());
    assertEquals(0, countMessages(entityId), "failing group must not create a message");
  }

  /** Asserts each group, except the failing ones, created exactly one message. */
  private void assertGroupsProcessedOnce(List<Long> ids, List<Long> failingIds) {
    for (Long id : ids) {
      if (!failingIds.contains(id)) {
        assertEquals(1, countMessages(id), "group should be processed exactly once");
      }
    }
  }

  /** Returns the audit log of the given entity, whatever its model (it may have been changed). */
  private AuditLog auditLogOf(Long entityId) {
    JPA.clear();
    return Query.of(AuditLog.class).filter("self.relatedId = ?", entityId).fetchOne();
  }

  private long countMessages(Long entityId) {
    return Query.of(MailMessage.class)
        .filter("self.relatedModel = :model AND self.relatedId = :id")
        .bind("model", AuditCheck.class.getName())
        .bind("id", entityId)
        .count();
  }

  private long countPendingAuditLogs() {
    return Query.of(AuditLog.class).filter("self.processed = false").count();
  }
}
