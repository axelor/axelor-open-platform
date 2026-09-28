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
import com.axelor.audit.db.AuditLog;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.AuditCheck;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests {@link AuditProcessor} pagination when pending audit logs span several batches. */
@GuiceModules(BaseAuditTest.AuditTestModule.class)
class AuditProcessorBatchTest extends BaseAuditTest {

  // Small batch size so tests span several batches
  private static final int BATCH_SIZE = 5;
  private static final int MAX_RETRY =
      AppSettings.get().getInt(AvailableAppSettings.AUDIT_LOGS_MAX_RETRY, 3);

  // Several full batches plus a partial one
  private static final int GROUP_COUNT = BATCH_SIZE * 2 + 3;

  @BeforeAll
  static void setBatchSize() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.AUDIT_PROCESSOR_BATCH_SIZE, String.valueOf(BATCH_SIZE));
  }

  @BeforeEach
  void cleanAuditLogs() {
    JPA.runInTransaction(() -> JPA.em().createQuery("DELETE FROM AuditLog").executeUpdate());
  }

  /** All groups of a transaction are processed, even when they span several batches. */
  @Test
  void shouldProcessAllGroupsOfTransactionAcrossBatches() throws Exception {
    var ids = asAdmin(() -> createTrackedInOneTransaction("Batch", GROUP_COUNT));
    assertEquals(GROUP_COUNT, countPendingAuditLogs());

    processAuditLogs();

    assertEquals(0, countPendingAuditLogs());
    for (Long id : ids) {
      assertNotNull(lastMessage(id));
    }
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
    for (Long id : ids) {
      assertNotNull(lastMessage(id));
    }
  }

  /** A failing group is tried once per run and doesn't prevent the other groups to be processed. */
  @Test
  void shouldTryFailingGroupOncePerRun() throws Exception {
    asAdmin(() -> createTrackedInOneTransaction("Failing", GROUP_COUNT));
    var failingLogId = breakFirstAuditLog(0);

    processAuditLogs();

    var failingLog = JPA.em().find(AuditLog.class, failingLogId);
    assertFalse(failingLog.getProcessed());
    assertEquals(1, failingLog.getRetryCount());
    assertEquals(1, countPendingAuditLogs(), "only the failing group should remain pending");
  }

  /**
   * Groups reaching their max retries leave the pending logs; the other groups must not be skipped.
   */
  @Test
  void shouldNotSkipGroupsWhenRetriesAreExhausted() throws Exception {
    asAdmin(() -> createTrackedInOneTransaction("Exhausted", GROUP_COUNT));
    // Break one group out of two, so some of them fall in full batches whatever the order
    var logIds =
        Query.of(AuditLog.class).order("id").fetch().stream().map(AuditLog::getId).toList();
    var failingLogIds = new ArrayList<Long>();
    for (int i = 0; i < logIds.size(); i += 2) {
      failingLogIds.add(breakAuditLog(logIds.get(i), MAX_RETRY - 1));
    }

    processAuditLogs();

    for (Long failingLogId : failingLogIds) {
      var failingLog = JPA.em().find(AuditLog.class, failingLogId);
      assertTrue(failingLog.getProcessed());
      assertEquals(MAX_RETRY, failingLog.getRetryCount());
    }
    assertEquals(0, countPendingAuditLogs(), "groups after the failing ones were skipped");
  }

  private List<Long> createTrackedInOneTransaction(String prefix, int count) {
    var ids = new ArrayList<Long>();
    JPA.runInTransaction(
        () -> {
          for (int i = 0; i < count; i++) {
            var entity = new AuditCheck();
            entity.setName(prefix + " " + i);
            JPA.em().persist(entity);
            ids.add(entity.getId());
          }
        });
    return ids;
  }

  /**
   * Makes the first group fail during processing by pointing it to an unknown model.
   *
   * @return the ID of the broken audit log
   */
  private Long breakFirstAuditLog(int retryCount) {
    var firstLog = Query.of(AuditLog.class).order("id").fetchOne();
    assertNotNull(firstLog);
    return breakAuditLog(firstLog.getId(), retryCount);
  }

  /**
   * Makes the group of the given audit log fail during processing by pointing it to an unknown
   * model.
   *
   * @return the ID of the broken audit log
   */
  private Long breakAuditLog(Long auditLogId, int retryCount) {
    JPA.runInTransaction(
        () ->
            JPA.em()
                .createQuery(
                    "UPDATE AuditLog SET relatedModel = :model, retryCount = :retry WHERE id = :id")
                .setParameter("model", "com.axelor.test.db.UnknownModel")
                .setParameter("retry", retryCount)
                .setParameter("id", auditLogId)
                .executeUpdate());
    JPA.clear();
    return auditLogId;
  }

  private long countPendingAuditLogs() {
    return Query.of(AuditLog.class).filter("self.processed = false").count();
  }
}
