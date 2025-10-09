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
 * <p>The class provides functionality to capture the current state, apply a previously captured
 * state to the executing thread, or restore the state to a previous snapshot or a cleared state.
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

  /** Applies the captured context state to the current thread. */
  public void apply() {
    TenantResolver.setCurrentTenant(tenantId);
    AuthUtils.setCurrentUser(user);
    AppFilter.setBaseURL(baseUrl);
    AppFilter.setLanguage(language);
  }

  /**
   * Restores the context state for the current thread. If a previously captured context state is
   * provided, the state is applied. If the provided state is null, the context is reset to a
   * cleared state.
   *
   * @param previous the previously captured {@code ContextState} to be restored; if null, the
   *     context will be reset to default values
   */
  public static void restore(ContextState previous) {
    if (previous == null) {
      TenantResolver.setCurrentTenant(null, null);
      AuthUtils.setCurrentUser(null);
      AppFilter.setBaseURL(null);
      AppFilter.setLanguage(null);
    } else {
      previous.apply();
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
