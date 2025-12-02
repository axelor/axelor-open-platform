/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTest;
import com.axelor.audit.db.AuditLog;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.cache.CaffeineTest;
import com.axelor.concurrent.ContextAware;
import com.axelor.db.audit.AuditModule;
import com.axelor.db.audit.AuditQueue;
import com.axelor.db.internal.DBHelper;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaSequence;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.AuditCheck;
import com.axelor.test.db.AuditCheckNoTracking;
import com.google.inject.persist.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@GuiceModules(AuditTest.AuditTestModule.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuditTest extends JpaTest {

  private static final int MAX_SIZE = 5000;
  private static final int BATCH_SIZE = DBHelper.getJdbcBatchSize();
  private static final int TOTAL_WARMUPS = 2;
  private static final int TOTAL_RUNS = 5;

  public static class AuditTestModule extends CaffeineTest.CaffeineTestModule {
    @Override
    protected void configure() {
      super.configure();
      install(new AuditModule());
    }
  }

  @AfterAll
  public static void afterAll() {
    // Wait for audit queue to be processed (5 minutes max)
    Beans.get(AuditQueue.class).await(5 * 60 * 1000);
  }

  @BeforeEach
  public void beforeAll() {
    if (Query.of(MetaSequence.class).count() == 0) {
      fixture("sequence-data.yml");
    }
    if (Query.of(User.class).count() == 0) {
      createUser();
    }
  }

  @Transactional
  void createUser() {
    User user = new User();
    user.setName("Administrator");
    user.setCode("admin");
    user.setPassword("password");
    getEntityManager().persist(user);
  }

  @Transactional
  void createEntity(String name, String email) {
    AuditCheck entity = new AuditCheck();
    entity.setName(name);
    entity.setEmail(email);
    getEntityManager().persist(entity);
  }

  @Transactional
  void updateEntity(AuditCheck entity, String name) {
    entity.setName(name);
    getEntityManager().persist(entity);
  }

  @Transactional
  void updateUser(User entity, String code) {
    entity.setCode(code);
    getEntityManager().persist(entity);
  }

  @Transactional
  void deleteUser(User entity) {
    getEntityManager().remove(entity);
  }

  @Test
  @Order(1)
  public void testInsert() {
    final Runnable job = () -> createEntity("Some NAME", "some.name@example.com");
    ContextAware.of().withTransaction(false).withUser(AuthUtils.getUser("admin")).build(job).run();

    AuditCheck entity = Query.of(AuditCheck.class).fetchOne();

    assertNotNull(entity);
    assertNotNull(entity.getId());
    assertNotNull(entity.getName());

    // check audit fields are set
    assertNotNull(entity.getCreatedOn());
    assertNotNull(entity.getCreatedBy());

    // check updated(On|By) fields are not set
    assertNull(entity.getUpdatedOn());
    assertNull(entity.getUpdatedBy());

    // check sequence field is set
    assertNotNull(entity.getEmpSeq());
  }

  @Test
  @Order(2)
  public void testUpdate() {
    final Runnable job = () -> createEntity("Another NAME", "another.name@example.com");
    ContextAware.of().withTransaction(false).withUser(AuthUtils.getUser("admin")).build(job).run();

    final AuditCheck entity =
        Query.of(AuditCheck.class).filter("self.name = ?", "Another NAME").fetchOne();

    assertNotNull(entity);

    final Integer lastVersion = entity.getVersion();

    final Runnable job2 = () -> updateEntity(entity, "New NAME");
    ContextAware.of().withTransaction(false).withUser(AuthUtils.getUser("admin")).build(job2).run();

    final AuditCheck updatedEntity =
        Query.of(AuditCheck.class).filter("self.name = ?", "New NAME").fetchOne();

    final Integer newVersion = updatedEntity.getVersion();

    assertEquals("New NAME", updatedEntity.getName());
    assertNotEquals(lastVersion, newVersion);

    // updated(On|By) fields should are set
    assertNotNull(updatedEntity.getUpdatedOn());
    assertNotNull(updatedEntity.getUpdatedBy());
  }

  @Test
  @Order(3)
  public void testUpdateUser() {
    User user = Query.of(User.class).filter("self.code = ?", "admin").fetchOne();
    assertThrows(PersistenceException.class, () -> updateUser(user, "administrator"));
  }

  @Test
  @Order(4)
  public void testDeleteUser() {
    User user = Query.of(User.class).filter("self.code = ?", "admin").fetchOne();
    assertThrows(PersistenceException.class, () -> deleteUser(user));
  }

  @Test
  @Order(5)
  public void testTrack() {
    List<AuditLog> auditLogs = Query.of(AuditLog.class).fetch();
    assertNotNull(auditLogs);
    assertNotEquals(0, auditLogs.size());
    assertTrue(
        auditLogs.stream().anyMatch(x -> AuditCheck.class.getName().equals(x.getRelatedModel())));
  }

  @Test
  @Order(6)
  void testClear() {
    EntityManager em = getEntityManager();
    assertDoesNotThrow(
        () -> {
          JPA.runInTransaction(
              () -> {
                AuditCheck entity = new AuditCheck();
                entity.setName("testClear");
                User user = JpaRepository.of(User.class).all().fetchOne();
                assertNotNull(user);
                entity.setUser(user);
                em.persist(entity);

                em.flush();
                em.clear();

                entity = em.find(AuditCheck.class, entity.getId());
                entity.setEmail("test@example.com");

                em.flush();
                em.clear();
              });
        });
  }

  @Test
  @Order(7)
  public void testPerformance() {
    if (System.getenv("AUDIT_PERF_TEST") == null) {
      System.out.println("Skipping Audit Performance Test (set AUDIT_PERF_TEST=1 to enable it)");
      return;
    }

    System.out.println("\n=== Audit Tracking Performance Test ===");
    System.out.printf(
        "Config: %d records, batch=%d, warmup=%d, runs=%d%n",
        MAX_SIZE, BATCH_SIZE, TOTAL_WARMUPS, TOTAL_RUNS);

    Function<String, AuditCheck> createAudit =
        name -> {
          AuditCheck entity = new AuditCheck();
          entity.setName(name);
          entity.setEmail(name + "@example.com");
          return entity;
        };

    Function<String, AuditCheckNoTracking> createBaseline =
        name -> {
          AuditCheckNoTracking entity = new AuditCheckNoTracking();
          entity.setName(name);
          entity.setEmail(name + "@example.com");
          return entity;
        };

    User user = AuthUtils.getUser("admin");
    if (user == null) {
      throw new RuntimeException("Admin user not found for performance test");
    }

    // Warmup with audit tracking (most expensive)
    System.out.println("\nWarming up JIT...");
    performTask(user, TOTAL_WARMUPS, AuditCheck.class, createAudit);

    // Measure with audit tracking
    System.out.println("\n[1/2] With audit tracking...");
    Statistics auditStats = performTask(user, TOTAL_RUNS, AuditCheck.class, createAudit);

    // Measure without audit tracking
    System.out.println("\n[2/2] Without audit tracking...");
    Statistics baselineStats =
        performTask(user, TOTAL_RUNS, AuditCheckNoTracking.class, createBaseline);

    // Print comparison
    auditStats.printComparison(baselineStats);
  }

  private <T extends Model> Statistics performTask(
      User user, int measurementRuns, Class<T> entityType, Function<String, T> createEntity) {
    var contextAware =
        ContextAware.of()
            .withTransaction(false)
            .withUser(user)
            .build(() -> performTask(measurementRuns, entityType, createEntity));
    try {
      return contextAware.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private <T extends Model> Statistics performTask(
      int measurementRuns, Class<T> entityType, Function<String, T> createEntity) {

    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

    long[] durations = new long[measurementRuns];
    for (int run = 0; run < measurementRuns; run++) {
      String prefix = "Run-" + run + "-";

      long startTime = System.nanoTime();
      JPA.runInTransaction(
          () -> {
            EntityManager em = getEntityManager();
            for (int i = 0; i < MAX_SIZE; i++) {
              String name = prefix + i;
              T entity = createEntity.apply(name);
              em.persist(entity);
              if ((i + 1) % BATCH_SIZE == 0) {
                em.flush();
                em.clear();
              }
            }
            em.flush();
            em.clear();
          });
      long endTime = System.nanoTime();

      durations[run] = (endTime - startTime) / 1_000_000;
      System.out.println("  Run " + (run + 1) + ": " + durations[run] + " ms");

      // Cleanup after each run
      JPA.runInTransaction(
          () -> {
            getEntityManager()
                .createQuery("DELETE FROM " + entityType.getSimpleName())
                .executeUpdate();
          });
      runtime.gc();
    }

    runtime.gc();
    long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

    Statistics stats = calculateStatistics(durations, MAX_SIZE);
    stats.memoryDelta = memoryAfter - memoryBefore;

    // Check if entity has audit tracking (check for AuditLog records)
    try {
      long auditCount =
          Query.of(AuditLog.class).filter("self.relatedModel = ?", entityType.getName()).count();
      stats.auditRecordCount = auditCount;
      stats.auditRecordsPerEntity = auditCount / (double) (MAX_SIZE * measurementRuns);
    } catch (Exception e) {
      // No audit tracking or query failed
      stats.auditRecordCount = 0;
      stats.auditRecordsPerEntity = 0;
    }

    return stats;
  }

  static class Statistics {
    // Timing
    long avgTime;
    long minTime;
    long maxTime;
    double stdDev;
    double avgPerRecord;
    double throughput;

    // Memory
    double memoryDelta;

    // Audit tracking (if applicable)
    long auditRecordCount;
    double auditRecordsPerEntity;

    // Test metadata
    int recordCount;
    int runs;

    void print() {
      System.out.printf(
          "  Time: %d ms (min=%d, max=%d, σ=%.1f)%n", avgTime, minTime, maxTime, stdDev);
      System.out.printf("  Throughput: %.0f rec/s (%.4f ms/rec)%n", throughput, avgPerRecord);
      System.out.printf("  Memory: %d MB%n", memoryDelta / 1024 / 1024);
      if (auditRecordCount > 0) {
        System.out.printf(
            "  Audit: %d records (%.2f per entity)%n", auditRecordCount, auditRecordsPerEntity);
      }
    }

    void printSummary(String label) {
      System.out.println(label + ":");
      System.out.printf(
          "  Time: %d ms (min=%d, max=%d, σ=%.1f)%n", avgTime, minTime, maxTime, stdDev);
      System.out.printf("  Throughput: %.0f rec/s (%.4f ms/rec)%n", throughput, avgPerRecord);
      System.out.printf("  Memory: %.2f MB%n", memoryDelta / 1024 / 1024);
      if (auditRecordCount > 0) {
        System.out.printf(
            "  Audit: %d records (%.2f per entity)%n", auditRecordCount, auditRecordsPerEntity);
      }
    }

    void printComparison(Statistics baseline) {
      System.out.println();
      baseline.printSummary("Without audit tracking");
      System.out.println();
      this.printSummary("With audit tracking");

      // Overhead calculation
      double timeOverheadPct = ((avgTime - baseline.avgTime) / (double) baseline.avgTime) * 100;
      double throughputDecreasePct =
          ((baseline.throughput - throughput) / baseline.throughput) * 100;
      double memoryOverheadMB = (memoryDelta - baseline.memoryDelta) / 1024 / 1024;

      System.out.println("\nOverhead:");
      System.out.printf("  Time: +%d ms (%.1f%%)%n", avgTime - baseline.avgTime, timeOverheadPct);
      System.out.printf(
          "  Throughput: -%.0f rec/s (%.1f%%)%n",
          baseline.throughput - throughput, throughputDecreasePct);
      System.out.printf("  Memory: +%.2f MB%n", memoryOverheadMB);
      System.out.printf("  Per-record: +%.4f ms%n", avgPerRecord - baseline.avgPerRecord);
      System.out.println();
    }
  }

  private Statistics calculateStatistics(long[] durations, int recordsPerRun) {
    Statistics stats = new Statistics();

    // Metadata
    stats.recordCount = recordsPerRun;
    stats.runs = durations.length;

    // Timing
    long sum = 0;
    stats.minTime = Long.MAX_VALUE;
    stats.maxTime = Long.MIN_VALUE;

    for (long duration : durations) {
      sum += duration;
      stats.minTime = Math.min(stats.minTime, duration);
      stats.maxTime = Math.max(stats.maxTime, duration);
    }

    stats.avgTime = sum / durations.length;
    stats.avgPerRecord = stats.avgTime / (double) recordsPerRun;
    stats.throughput = 1000.0 / stats.avgPerRecord;

    // Standard deviation
    double variance = 0;
    for (long duration : durations) {
      variance += Math.pow(duration - stats.avgTime, 2);
    }
    stats.stdDev = Math.sqrt(variance / durations.length);

    return stats;
  }
}
