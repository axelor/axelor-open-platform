/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.concurrent;

import com.axelor.auth.db.User;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * A utility class for creating context-aware wrappers around {@link Runnable} and {@link Callable}
 * tasks. This class provides convenience methods to encapsulate tasks in a context-aware execution
 * environment, which propagates contextual information such as tenant and user, base URL, and
 * language.
 *
 * <pre>{@code
 * // Capture current thread context and run in background
 * executor.submit(
 *     ContextAware.of()
 *         .withTransaction(true)
 *         .tenantId("tenant42")
 *         .language(Locale.FRENCH)
 *         .build(() -> service.processRequest())
 * );
 *
 * // Callable example
 * Future<User> future = executor.submit(
 *     ContextAware.of()
 *         .withTransaction(false)
 *         .tenantId("tenant42")
 *         .build(() -> userService.findById(123))
 * );
 * }</pre>
 */
public final class ContextAware {
  protected String tenantId;
  protected User user;
  protected String baseUrl;
  protected Locale language;
  protected boolean withTransaction = true;

  private ContextAware() {
    this(ContextState.capture());
  }

  private ContextAware(ContextState contextState) {
    this(
        contextState.getTenantId(),
        contextState.getUser(),
        contextState.getBaseUrl(),
        contextState.getLanguage());
  }

  private ContextAware(String tenantId, User user, String baseUrl, Locale language) {
    this.tenantId = tenantId;
    this.user = user;
    this.baseUrl = baseUrl;
    this.language = language;
  }

  /**
   * Creates a {@code ContextAware} instance for building a context-aware task.
   *
   * @return context-aware task builder
   */
  public static ContextAware of() {
    return new ContextAware();
  }

  /**
   * Creates a {@code ContextAware} instance for building a context-aware task.
   *
   * @param tenantId the tenant identifier
   * @return context-aware task builder
   */
  public static ContextAware of(String tenantId) {
    return new ContextAware().withTenantId(tenantId);
  }

  /**
   * Creates a {@code ContextAware} instance for building a context-aware task.
   *
   * @param tenantId the tenant identifier
   * @param user the user associated with the context
   * @return context-aware task builder
   */
  public static ContextAware of(String tenantId, User user) {
    return new ContextAware().withTenantId(tenantId).withUser(user);
  }

  /*
   * Creates a {@code ContextAware} instance for building a context-aware task.
   *
   * @param tenantId the tenant identifier
   * @param user the user associated with the context
   * @param baseUrl the base URL for the context
   * @param language the locale representing the language preference
   * @return context-aware task builder
   */
  public static ContextAware of(String tenantId, User user, String baseUrl, Locale language) {
    return new ContextAware(tenantId, user, baseUrl, language);
  }

  /*
   * Creates a {@code ContextAware} instance for building a context-aware task.
   *
   * @param tenantId the tenant identifier
   * @param user the user associated with the context
   * @param baseUrl the base URL for the context
   * @param language the locale representing the language preference
   * @param withTransaction whether to start a transaction
   * @return context-aware task builder
   */
  public static ContextAware of(
      String tenantId, User user, String baseUrl, Locale language, boolean withTransaction) {
    return new ContextAware(tenantId, user, baseUrl, language).withTransaction(withTransaction);
  }

  /**
   * Sets the tenant identifier for the context-aware task.
   *
   * @param tenantId
   * @return context-aware task builder
   */
  public ContextAware withTenantId(String tenantId) {
    this.tenantId = tenantId;
    return this;
  }

  /**
   * Sets the user for the context-aware task.
   *
   * @param user
   * @return context-aware task builder
   */
  public ContextAware withUser(User user) {
    this.user = user;
    return this;
  }

  /**
   * Sets the base URL for the context-aware task.
   *
   * @param baseUrl the base URL for the context
   * @return context-aware task builder
   */
  public ContextAware withBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
    return this;
  }

  /**
   * Sets the language preference for the context-aware task.
   *
   * @param language the locale representing the language preference
   * @return context-aware task builder
   */
  public ContextAware withLanguage(Locale language) {
    this.language = language;
    return this;
  }

  /**
   * Sets the transactional flag for the context-aware task.
   *
   * @param withTransaction whether to start a transaction
   * @return context-aware task builder
   */
  public ContextAware withTransaction(boolean withTransaction) {
    this.withTransaction = withTransaction;
    return this;
  }

  /**
   * Builds a {@code ContextAwareCallable} instance from the given {@link Callable} task and the
   * context from the current {@code ContextAware} instance.
   *
   * @param <V>
   * @param callable task
   * @return context-aware callable task
   */
  public <V> ContextAwareCallable<V> build(Callable<V> callable) {
    return new ContextAwareCallable<>(
        callable, ContextState.of(tenantId, user, baseUrl, language), withTransaction);
  }

  /**
   * Builds a {@code ContextAwareRunnable} instance from the given {@link Runnable} task and the
   * context from the current {@code ContextAware} instance.
   *
   * @param runnable
   * @return context-aware runnable task
   */
  public ContextAwareRunnable build(Runnable runnable) {
    return new ContextAwareRunnable(
        runnable, ContextState.of(tenantId, user, baseUrl, language), withTransaction);
  }
}
