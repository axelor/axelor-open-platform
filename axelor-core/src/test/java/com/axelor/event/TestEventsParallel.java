/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.axelor.test.GuiceExtension;
import com.axelor.test.GuiceModules;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceExtension.class)
@GuiceModules({EventModule.class, TestModule.class})
public class TestEventsParallel {

  private static final AtomicInteger count = new AtomicInteger();

  @Inject private Event<ParallelEvent> parallelEvent;

  void onParallel0(@Observes @Named("parallel") ParallelEvent event) {
    count.incrementAndGet();
  }

  void onParallel1(@Observes @Named("parallel") ParallelEvent event) {
    count.incrementAndGet();
  }

  void onParallel2(@Observes @Named("parallel") ParallelEvent event) {
    count.incrementAndGet();
  }

  void onParallel3(@Observes @Named("parallel") ParallelEvent event) {
    count.incrementAndGet();
  }

  @Test
  public void testParallel() {
    final int numWorkers = 4;
    final int numIterations = 4;
    final int numObservers = 4;
    final ExecutorService pool = Executors.newFixedThreadPool(numWorkers);

    for (int i = 0; i < numIterations; ++i) {
      pool.execute(
          () -> parallelEvent.select(NamedLiteral.of("parallel")).fire(new ParallelEvent()));
    }

    shutdownAndAwaitTermination(pool);
    assertEquals(numIterations * numObservers, count.getAndUpdate(n -> 0));
  }

  private static void shutdownAndAwaitTermination(ExecutorService pool) {
    pool.shutdown();

    try {
      while (!pool.awaitTermination(1, TimeUnit.HOURS)) {
        System.err.printf("Pool %s takes a long time to terminate.%n", pool);
      }
    } catch (InterruptedException e) {
      pool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private static class ParallelEvent {}
}
