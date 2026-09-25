/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.concurrent;

import com.axelor.db.JPA;

/**
 * A wrapper around a {@link Runnable} that enables execution within a context-aware environment.
 * This propagates contextual information such as tenant and user details, as well as the base URL
 * and language preferences, to the execution thread of the task.
 */
public final class ContextAwareRunnable implements Runnable {

  private final Runnable task;
  private final ContextState contextState;

  ContextAwareRunnable(Runnable task, ContextState contextState, boolean withTransaction) {
    this.task = wrapTransaction(task, withTransaction);
    this.contextState = contextState;
  }

  /**
   * Creates a new instance of {@link ContextAwareRunnable} for the given {@link Runnable} task.
   *
   * @param task the {@link Runnable} task to be executed within a context-aware environment
   * @return a {@link ContextAwareRunnable} instance wrapping the provided task
   */
  public static ContextAwareRunnable of(Runnable task) {
    return of(task, true);
  }

  /**
   * Creates a new instance of {@link ContextAwareRunnable} for the given {@link Runnable} task.
   *
   * @param task the {@link Runnable} task to be executed within a context-aware environment
   * @param withTransaction whether the task should be executed within a transactional scope
   * @return a {@link ContextAwareRunnable} instance wrapping the provided task
   */
  public static ContextAwareRunnable of(Runnable task, boolean withTransaction) {
    return new ContextAwareRunnable(task, ContextState.capture(), withTransaction);
  }

  /**
   * Executes the enclosed {@link Runnable} task within a context-aware environment, applying the
   * captured context state and managing a transactional scope if specified.
   */
  @Override
  public void run() {
    final ContextState previous = ContextState.capture();
    contextState.apply();

    try (ContextUnitOfWork ignored = ContextUnitOfWork.begin()) {
      task.run();
    } finally {
      ContextState.restore(previous);
    }
  }

  private static Runnable wrapTransaction(Runnable task, boolean withTransaction) {
    return withTransaction ? () -> JPA.runInTransaction(task::run) : task;
  }
}
