/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.concurrent;

import com.axelor.db.JPA;
import java.util.concurrent.Callable;

/**
 * A wrapper around a {@link Callable} that enables execution within a context-aware environment.
 * This propagates contextual information such as tenant and user details, as well as the base URL
 * and language preferences, to the execution thread of the task.
 *
 * @param <V> the result type of the enclosed {@link Callable}
 */
public final class ContextAwareCallable<V> implements Callable<V> {

  private final Callable<V> task;
  private final ContextState contextState;

  ContextAwareCallable(Callable<V> task, ContextState contextState, boolean withTransaction) {
    this.task = wrapTransaction(task, withTransaction);
    this.contextState = contextState;
  }

  /**
   * Creates a {@code ContextAwareCallable} instance from the given {@link Callable} task. The
   * returned {@code ContextAwareCallable} enables execution of the task in a context-aware
   * environment.
   *
   * @param <V> the result type of the enclosed {@link Callable}
   * @param task the {@link Callable} to be wrapped into a {@code ContextAwareCallable}
   * @return a new {@code ContextAwareCallable} instance encapsulating the provided task
   */
  public static <V> ContextAwareCallable<V> of(Callable<V> task) {
    return of(task, true);
  }

  /**
   * Creates a {@code ContextAwareCallable} instance from the given {@link Callable} task. The
   * returned {@code ContextAwareCallable} enables execution of the task in a context-aware
   * environment.
   *
   * @param <V> the result type of the enclosed {@link Callable}
   * @param task the {@link Callable} to be wrapped into a {@code ContextAwareCallable}
   * @param withTransaction whether the task should be executed within a transactional scope
   * @return a new {@code ContextAwareCallable} instance encapsulating the provided task
   */
  public static <V> ContextAwareCallable<V> of(Callable<V> task, boolean withTransaction) {
    return new ContextAwareCallable<>(task, ContextState.capture(), withTransaction);
  }

  @Override
  public V call() throws Exception {
    final ContextState previous = ContextState.capture();
    contextState.apply();
    try {
      return task.call();
    } finally {
      ContextState.restore(previous);
    }
  }

  private static <V> Callable<V> wrapTransaction(Callable<V> task, boolean withTransaction) {
    return withTransaction
        ? () ->
            JPA.callInTransaction(
                () -> {
                  try {
                    return task.call();
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                })
        : task;
  }
}
