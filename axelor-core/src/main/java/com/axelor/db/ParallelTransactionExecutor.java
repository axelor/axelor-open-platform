/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.db;

import java.lang.invoke.MethodHandles;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Executes transactions in parallel and handles rollback if any exception occurs. */
public class ParallelTransactionExecutor {

  private final int numWorkers;
  private final ExecutorService workerPool;
  private final List<Future<?>> workerFutures;
  private final ConcurrentMap<Integer, Entry<CountDownLatch, Queue<Runnable>>> commandsByPriority;
  private final List<Entry<CountDownLatch, Queue<Runnable>>> commands;
  private boolean rollbackNeeded;

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Instantiates a parallel transaction executor with as many workers as there are available
   * processors.
   */
  public ParallelTransactionExecutor() {
    this(Runtime.getRuntime().availableProcessors());
  }

  /**
   * Instantiates a parallel transaction executor with the specified number of workers.
   *
   * @param numWorkers
   */
  public ParallelTransactionExecutor(int numWorkers) {
    this.numWorkers = numWorkers;
    workerPool = Executors.newFixedThreadPool(numWorkers);
    workerFutures = new ArrayList<>(numWorkers);
    commandsByPriority = new ConcurrentHashMap<>();
    commands = new ArrayList<>();
  }

  /**
   * Adds a command to the queue of commands to run in a transaction.
   *
   * @param command
   */
  public void add(Runnable command) {
    add(command, 0);
  }

  /**
   * Adds a command to the queue of commands to run in a transaction with the specified priority.
   * Commands with the same priority will all be completed before moving on to commands with
   * different priorities.
   *
   * @param command
   * @param priority
   */
  public void add(Runnable command, int priority) {
    commandsByPriority
        .computeIfAbsent(
            priority,
            key ->
                new SimpleImmutableEntry<>(
                    new CountDownLatch(numWorkers), new ConcurrentLinkedQueue<>()))
        .getValue()
        .add(command);
  }

  /**
   * Runs the commands in parallel transactions and wait for completion. If an exception occurs in
   * any command, all transactions are rolled back.
   */
  public void run() {
    start();
    waitAndShutdown();
  }

  private void start() {
    commandsByPriority
        .keySet()
        .stream()
        .sorted()
        .forEachOrdered(priority -> commands.add(commandsByPriority.get(priority)));

    for (int i = 0; i < numWorkers; ++i) {
      workerFutures.add(workerPool.submit(this::runCommandsInTransaction));
    }
  }

  private void waitAndShutdown() {
    try {
      workerFutures.forEach(
          future -> {
            try {
              wait(future);
            } catch (InterruptedException e) {
              logger.error(e.getMessage(), e);
              Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
              final Throwable cause = e.getCause();

              if (cause instanceof ErrorInAnotherWorker) {
                return;
              } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
              } else if (cause instanceof Error) {
                throw (Error) cause;
              } else {
                // Should never happen
                throw new IllegalStateException(cause);
              }
            }
          });
    } finally {
      workerPool.shutdown();
    }
  }

  private void wait(Future<?> future) throws InterruptedException, ExecutionException {
    do {
      try {
        future.get(1, TimeUnit.HOURS);
        return;
      } catch (TimeoutException e) {
        logger.warn("Furure {} of pool {} is taking a long time to complete.", future, workerPool);
      }
    } while (!Thread.currentThread().isInterrupted());
  }

  private void runCommandsInTransaction() {
    JPA.runInTransaction(
        () -> {
          RuntimeException error = null;

          for (final Entry<CountDownLatch, Queue<Runnable>> entry : commands) {
            final CountDownLatch doneSignal = entry.getKey();
            final Queue<Runnable> commandQueue = entry.getValue();

            try {
              for (Runnable command; (command = commandQueue.poll()) != null; ) {
                command.run();
              }
            } catch (RuntimeException e) {
              rollbackNeeded = true;
              error = e;
              clearCommandQueues();
            } finally {
              doneSignal.countDown();
            }

            try {
              doneSignal.await();
            } catch (InterruptedException e) {
              logger.error(e.getMessage(), e);
              Thread.currentThread().interrupt();
            }
          }

          if (error != null) {
            throw error;
          } else if (rollbackNeeded) {
            throw new ErrorInAnotherWorker();
          }
        });
  }

  private void clearCommandQueues() {
    commands.parallelStream().map(Entry::getValue).forEach(Collection::clear);
  }

  private static class ErrorInAnotherWorker extends RuntimeException {
    private static final long serialVersionUID = 5171709514475835079L;
  }
}
