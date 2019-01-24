/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.event;

import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Named;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GuiceRunner.class)
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
    Assert.assertEquals(numIterations * numObservers, count.getAndUpdate(n -> 0));
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
