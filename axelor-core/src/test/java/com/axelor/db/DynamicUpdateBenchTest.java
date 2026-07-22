/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.axelor.JpaTestModule;
import com.axelor.test.GuiceExtension;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.Bench100Dynamic;
import com.axelor.test.db.Bench100Static;
import com.axelor.test.db.Bench10Dynamic;
import com.axelor.test.db.Bench10Static;
import com.axelor.test.db.Bench200Dynamic;
import com.axelor.test.db.Bench200Static;
import com.axelor.test.db.Bench50Dynamic;
import com.axelor.test.db.Bench50Static;
import com.axelor.test.db.Bench75Dynamic;
import com.axelor.test.db.Bench75Static;
import com.axelor.test.db.BenchLobDynamic;
import com.axelor.test.db.BenchLobStatic;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Benchmark measuring Hibernate {@code @DynamicUpdate} performance across two sweeps:
 *
 * <ul>
 *   <li><b>Width sweep</b> ({@link #runWidthSweep}): increasing column count (no CLOB/BLOB) to
 *       locate where trimming unchanged columns outweighs per-flush SQL generation.
 *   <li><b>LOB payload sweep</b> ({@link #runLobSweep}): varying CLOB/BLOB size on a small entity
 *       to locate where omitting large unchanged columns pays off.
 * </ul>
 *
 * <p>Each sweep compares static vs. dynamic update entities across two update shapes:
 *
 * <ul>
 *   <li><b>partial</b>: updates up to two randomly chosen small columns per flush so dynamic SQL
 *       varies, preventing PreparedStatement cache hits. This is deliberately pessimistic for
 *       dynamic updates: a real workload dirties a repeatable set of columns, so it sees a small
 *       number of stable SQL shapes and better cache behaviour than measured here.
 *   <li><b>full</b>: updates every column, isolating per-flush SQL generation cost. Nothing is left
 *       unchanged, so the payload sweeps have nothing to trim and are expected to show parity.
 * </ul>
 *
 * <p><b>Results are database-dependent; run against PostgreSQL, not in-memory HSQLDB.</b>
 * {@code @DynamicUpdate} trades {@code PreparedStatement} cache hits (server-side SQL parsing) for
 * fewer bound columns and reduced wire I/O. In-process HSQLDB lacks network and prepare overhead,
 * so dynamic updates always appear faster. On PostgreSQL, dynamic updates hurt narrow entities due
 * to cache misses, but benefit entities carrying large, rarely-changed CLOB/BLOB fields.
 *
 * <p>Run with:
 *
 * <pre>
 *   DYNAMIC_UPDATE_PERF_TEST=1 ./gradlew :axelor-core:test --tests '*DynamicUpdateBenchTest*' -i
 * </pre>
 *
 * <p>Interleaves trials per round and reports medians to reduce database transient noise.
 */
@ExtendWith(GuiceExtension.class)
@GuiceModules({JpaTestModule.class})
class DynamicUpdateBenchTest {

  /** Number of updates per benchmark run. */
  private static final int ITERATIONS = 2000;

  /** Number of warmup iterations. */
  private static final int WARMUP = 200;

  /** Measured passes per config, interleaved per round to avoid transient DB skew. */
  private static final int TRIALS = 10;

  /** Flushes in transaction batches to prevent uncommitted memory growth. */
  private static final int BATCH_SIZE = 200;

  /** CLOB/BLOB sizes (bytes) swept by {@link #runLobSweep}. */
  private static final int[] PAYLOAD_BYTES_STEPS = {0, 16, 1024, 16384, 131072};

  private static final RandomGenerator RNG = RandomGenerator.getDefault();

  /** Caches {@code setFieldNN} setters per entity class. */
  private final Map<Class<?>, Method[]> setterCache = new HashMap<>();

  /** Name of the database under test. */
  private String database = "unknown DB";

  @Test
  @EnabledIfEnvironmentVariable(named = "DYNAMIC_UPDATE_PERF_TEST", matches = "(?i)1|true|yes|on")
  void benchmark() {
    database = detectDatabase();

    assertDoesNotThrow(
        () -> {
          runWidthSweep();
          runLobSweep();
        });
  }

  private String detectDatabase() {
    return JPA.callInTransaction(
        () ->
            JPA.em()
                .unwrap(Session.class)
                .doReturningWork(conn -> conn.getMetaData().getDatabaseProductName()));
  }

  // Column-count threshold sweep (no CLOB/BLOB)

  private void runWidthSweep() {
    List<Row> rows =
        List.of(
            widthRow(10, Bench10Static.class, Bench10Dynamic.class),
            widthRow(50, Bench50Static.class, Bench50Dynamic.class),
            widthRow(75, Bench75Static.class, Bench75Dynamic.class),
            widthRow(100, Bench100Static.class, Bench100Dynamic.class),
            widthRow(200, Bench200Static.class, Bench200Dynamic.class));

    measure(rows);

    printSweep(
        "Field-count threshold sweep (locates the >N rule)",
        String.format(
            "%d updates, no CLOB/BLOB, %s, median of %d interleaved trials",
            ITERATIONS, database, TRIALS),
        "Columns",
        "(ratio < 1 means @DynamicUpdate is faster; partial = 2 random columns, full = all"
            + " columns)",
        rows);
  }

  private Row widthRow(
      int columns,
      Class<? extends Model> staticUpdateModel,
      Class<? extends Model> dynamicUpdateModel) {
    Consumer<Model> noSeed = e -> {};
    return new Row(
        columns,
        bench(staticUpdateModel, noSeed, partialMutator()),
        bench(dynamicUpdateModel, noSeed, partialMutator()),
        bench(staticUpdateModel, noSeed, fullMutator()),
        bench(dynamicUpdateModel, noSeed, fullMutator()));
  }

  // Large CLOB/BLOB payload sweep (large/binary trigger)

  private void runLobSweep() {
    List<Row> rows = new ArrayList<>();
    for (int bytes : PAYLOAD_BYTES_STEPS) {
      rows.add(lobRow(bytes));
    }

    measure(rows);

    printSweep(
        "CLOB/BLOB payload sweep (large/binary rule)",
        String.format(
            "%d updates, 10-column entity + CLOB & BLOB of the given size, %s, "
                + "median of %d interleaved trials",
            ITERATIONS, database, TRIALS),
        "Bytes",
        "(ratio < 1 means @DynamicUpdate is faster; partial = 2 random columns, full = all columns"
            + " + LOBs)",
        rows);
  }

  private Row lobRow(int bytes) {
    return new Row(
        bytes,
        bench(BenchLobStatic.class, lobFullMutator(bytes), partialMutator()),
        bench(BenchLobDynamic.class, lobFullMutator(bytes), partialMutator()),
        bench(BenchLobStatic.class, lobFullMutator(bytes), lobFullMutator(bytes)),
        bench(BenchLobDynamic.class, lobFullMutator(bytes), lobFullMutator(bytes)));
  }

  // Measurement

  private void measure(List<Row> rows) {
    List<Bench> configs = new ArrayList<>();
    for (Row r : rows) {
      configs.add(r.partialStatic());
      configs.add(r.partialDyn());
      configs.add(r.fullStatic());
      configs.add(r.fullDyn());
    }

    // Warm up JVM, JDBC, and connection pools.
    for (Bench b : configs) {
      time(b, WARMUP);
    }

    // Interleave trials per round to reduce DB noise.
    for (int t = 0; t < TRIALS; t++) {
      for (Bench b : configs) {
        b.samples[t] = time(b, ITERATIONS);
      }
    }
  }

  private Bench bench(Class<? extends Model> cls, Consumer<Model> seed, Consumer<Model> mutator) {
    Supplier<Model> factory = () -> instantiate(cls);
    return new Bench(cls, factory, seed, mutator);
  }

  /**
   * Dirties two random string columns per flush to vary the dynamic SQL and avoid PreparedStatement
   * cache hits.
   */
  private Consumer<Model> partialMutator() {
    return model -> {
      Method[] setters = fieldSetters(model.getClass());
      if (setters.length < 2) {
        return;
      }
      int a = RNG.nextInt(setters.length);
      int b = RNG.nextInt(setters.length);
      invokeSet(setters[a], model, randomStr());
      if (b != a) {
        invokeSet(setters[b], model, randomStr());
      }
    };
  }

  /** Dirties every small string column. */
  private Consumer<Model> fullMutator() {
    return this::setAllStrings;
  }

  /** Dirties all small columns plus the CLOB and BLOB. */
  private Consumer<Model> lobFullMutator(int bytes) {
    return model -> {
      setAllStrings(model);
      fillPayload(model, bytes);
    };
  }

  /** Fills the CLOB and BLOB with {@code bytes} of random content. */
  private void fillPayload(Object entity, int bytes) {
    byte[] data = new byte[bytes];
    RNG.nextBytes(data);
    char[] chars = new char[bytes];
    for (int i = 0; i < bytes; i++) {
      chars[i] = (char) ('a' + RNG.nextInt(26));
    }
    String text = new String(chars);
    if (entity instanceof BenchLobStatic e) {
      e.setLargeString(text);
      e.setBinaryData(data);
    } else if (entity instanceof BenchLobDynamic e) {
      e.setLargeString(text);
      e.setBinaryData(data);
    } else {
      throw new IllegalArgumentException("Unsupported entity: " + entity.getClass().getName());
    }
  }

  private static Model instantiate(Class<? extends Model> cls) {
    try {
      return cls.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  // Timing harness

  /** Applies {@code iterations} mutations, in {@link #BATCH_SIZE} batches, returning elapsed ns. */
  private long time(Bench bench, int iterations) {
    Long id =
        JPA.callInTransaction(
            () -> {
              EntityManager em = JPA.em();
              Model entity = bench.factory.get();
              bench.seed.accept(entity);
              em.persist(entity);
              em.flush();
              return entity.getId();
            });

    long elapsed = 0;
    for (int done = 0; done < iterations; done += BATCH_SIZE) {
      int batch = Math.min(BATCH_SIZE, iterations - done);
      // Time only the mutation and flush loop, excluding reload/commit.
      elapsed +=
          JPA.callInTransaction(
              () -> {
                EntityManager em = JPA.em();
                Model entity = em.find(bench.type, id);
                long start = System.nanoTime();
                for (int i = 0; i < batch; i++) {
                  bench.mutator.accept(entity);
                  em.flush();
                }
                return System.nanoTime() - start;
              });
    }

    JPA.runInTransaction(
        () -> {
          EntityManager em = JPA.em();
          Model entity = em.find(bench.type, id);
          if (entity != null) {
            em.remove(entity);
          }
        });
    return elapsed;
  }

  /** Benchmark configuration. */
  private static class Bench {
    final Class<? extends Model> type;
    final Supplier<Model> factory;
    final Consumer<Model> seed;
    final Consumer<Model> mutator;
    final long[] samples = new long[TRIALS];

    Bench(
        Class<? extends Model> type,
        Supplier<Model> factory,
        Consumer<Model> seed,
        Consumer<Model> mutator) {
      this.type = type;
      this.factory = factory;
      this.seed = seed;
      this.mutator = mutator;
    }

    long median() {
      long[] sorted = samples.clone();
      Arrays.sort(sorted);
      int mid = sorted.length / 2;
      return sorted.length % 2 == 0 ? (sorted[mid - 1] + sorted[mid]) / 2 : sorted[mid];
    }
  }

  /** The four configs measured at one sweep step; {@code label} is the row's varied dimension. */
  private record Row(
      int label, Bench partialStatic, Bench partialDyn, Bench fullStatic, Bench fullDyn) {}

  // Output helpers

  private void printSweep(
      String title, String subtitle, String labelHeader, String footer, List<Row> rows) {
    System.out.printf("%n=== %s ===%n", title);
    System.out.printf("(%s)%n", subtitle);
    System.out.printf(
        "%-8s %18s %18s %8s   %18s %18s %8s%n",
        labelHeader,
        "partial static(µs)",
        "partial dyn(µs)",
        "ratio",
        "full static(µs)",
        "full dyn(µs)",
        "ratio");
    System.out.println("-".repeat(104));
    for (Row r : rows) {
      printRow(r);
    }
    System.out.println(footer);
  }

  private void printRow(Row r) {
    long partialStatic = r.partialStatic().median();
    long partialDyn = r.partialDyn().median();
    long fullStatic = r.fullStatic().median();
    long fullDyn = r.fullDyn().median();
    System.out.printf(
        "%-8d %18.1f %18.1f %8.2f   %18.1f %18.1f %8.2f%n",
        r.label(),
        perUpdateMicros(partialStatic),
        perUpdateMicros(partialDyn),
        (double) partialDyn / partialStatic,
        perUpdateMicros(fullStatic),
        perUpdateMicros(fullDyn),
        (double) fullDyn / fullStatic);
  }

  private double perUpdateMicros(long nanos) {
    return (nanos / 1_000.0) / ITERATIONS;
  }

  // Reflective field helpers

  private void setAllStrings(Object entity) {
    for (Method m : fieldSetters(entity.getClass())) {
      invokeSet(m, entity, randomStr());
    }
  }

  private static void invokeSet(Method setter, Object entity, Object value) {
    try {
      setter.invoke(entity, value);
    } catch (ReflectiveOperationException ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Random string used to dirty a column. */
  private static String randomStr() {
    return Long.toHexString(RNG.nextLong());
  }

  private Method[] fieldSetters(Class<?> cls) {
    return setterCache.computeIfAbsent(
        cls,
        c ->
            Arrays.stream(c.getMethods())
                .filter(m -> m.getName().startsWith("setField"))
                .sorted(Comparator.comparing(Method::getName))
                .toArray(Method[]::new));
  }
}
