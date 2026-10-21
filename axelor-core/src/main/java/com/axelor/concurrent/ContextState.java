/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.concurrent;

import com.axelor.app.internal.AppFilter;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.tenants.TenantResolver;
import java.util.Locale;

/**
 * ContextState represents a snapshot of contextual information associated with the current thread,
 * including tenant and user details, base URL, and language preferences.
 *
 * <p>The class provides functionality to capture the current state and apply it to the executing
 * thread, within a scope that restores the thread's previous state when closed.
 */
final class ContextState {

  private final String tenantId;
  private final User user;
  private final String baseUrl;
  private final Locale language;

  private ContextState(String tenantId, User user, String baseUrl, Locale language) {
    this.tenantId = tenantId;
    this.user = user;
    this.baseUrl = baseUrl;
    this.language = language;
  }

  /**
   * Creates a new instance of {@code ContextState} with the provided contextual information.
   *
   * @param tenantId the identifier of the tenant
   * @param user the user associated with the context
   * @param baseUrl the base URL for the context
   * @param language the locale representing the language preference
   * @return a new {@code ContextState} instance containing the provided contextual data
   */
  public static ContextState of(String tenantId, User user, String baseUrl, Locale language) {
    return new ContextState(tenantId, user, baseUrl, language);
  }

  /**
   * Captures the current context state of the executing thread.
   *
   * @return a {@code ContextState} instance encapsulating the current contextual information
   */
  public static ContextState capture() {
    return new ContextState(
        TenantResolver.currentTenantIdentifier(),
        AuthUtils.getUser(),
        AppFilter.getBaseURL(),
        AppFilter.getLanguage());
  }

  /**
   * Applies the captured context state to the current thread.
   *
   * <p>The user found before applying is saved from the thread-local alone, never resolved through
   * the session, so closing the returned scope puts it back exactly as it was. Restoring the
   * session-resolved user instead would pin it into a thread-local that was unset, where it
   * outlives the request on the pooled thread and leaks into the next request that thread serves.
   *
   * @return a scope restoring the previous thread-local state when closed
   */
  public Scope apply() {
    final Scope previous = new Scope();
    try {
      TenantResolver.setCurrentTenant(tenantId);
      AuthUtils.setCurrentUser(user);
      AppFilter.setBaseURL(baseUrl);
      AppFilter.setLanguage(language);
    } catch (Throwable e) {
      previous.close();
      throw e;
    }
    return previous;
  }

  /** The thread state saved by {@link #apply()}, restored when closed. */
  static final class Scope implements AutoCloseable {

    private final String tenantId;
    private final String tenantHost;
    private final User user;
    private final String baseUrl;
    private final Locale language;

    private Scope() {
      this.tenantId = TenantResolver.currentTenantIdentifier();
      this.tenantHost = TenantResolver.currentTenantHost();
      this.user = AuthUtils.getCurrentUser();
      this.baseUrl = AppFilter.getBaseURL();
      this.language = AppFilter.getLanguage();
    }

    @Override
    public void close() {
      // Restore the saved host: on a request thread it comes from the Host header and may differ
      // from the host setCurrentTenant(tenantId) would look up in the tenant config.
      TenantResolver.setCurrentTenant(tenantId, tenantHost);
      if (user == null) {
        AuthUtils.removeCurrentUser();
      } else {
        AuthUtils.setCurrentUser(user);
      }
      AppFilter.setBaseURL(baseUrl);
      AppFilter.setLanguage(language);
    }
  }

  // === getters ===

  public String getTenantId() {
    return tenantId;
  }

  public User getUser() {
    return user;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public Locale getLanguage() {
    return language;
  }
}
