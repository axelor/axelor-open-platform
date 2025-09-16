/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.tenants;

import com.axelor.app.internal.AppFilter;
import com.axelor.db.JPA;
import java.util.Locale;

/**
 * A Thread implementation that makes a thread tenant-aware by setting the tenant configuration
 * before executing its task and clearing it afterward.
 *
 * <p>This class ensures that any operations performed in the thread are associated with the correct
 * tenant. It uses a tenant resolver (e.g., {@link TenantResolver}) to set and manage the tenant
 * information during thread execution.
 *
 * <p>By default, it will run the task inside a new transaction.
 */
public class TenantAware extends Thread {

  /** The tenant identifier */
  private String tenantId;

  /** The tenant host */
  private String tenantHost;

  /* request base url */
  private String baseUrl;

  /* request language */
  private Locale language;

  /** Whether to start a new transaction */
  private boolean withTransaction;

  /**
   * Constructs a TenantAware using the current tenant.
   *
   * @param task the task to execute
   */
  public TenantAware(Runnable task) {
    super(task);
    this.tenantId = TenantResolver.currentTenantIdentifier();
    this.tenantHost = TenantResolver.currentTenantHost();

    this.baseUrl = AppFilter.getBaseURL();
    this.language = AppFilter.getLanguage();

    this.withTransaction = true;
  }

  /**
   * Specify the tenant identifier
   *
   * @param tenantId the tenant identifier
   * @return this
   */
  public TenantAware tenantId(String tenantId) {
    this.tenantId = tenantId;
    return this;
  }

  /**
   * Specify the tenant host
   *
   * @param tenantHost the tenant host
   * @return this
   */
  public TenantAware tenantHost(String tenantHost) {
    this.tenantHost = tenantHost;
    return this;
  }

  /**
   * Whether the task should run inside a new transaction
   *
   * @param withTransaction false to not open a transaction, else true
   * @return this
   */
  public TenantAware withTransaction(boolean withTransaction) {
    this.withTransaction = withTransaction;
    return this;
  }

  @Override
  public void run() {
    TenantResolver.setCurrentTenant(tenantId, tenantHost);
    AppFilter.setBaseURL(baseUrl);
    AppFilter.setLanguage(language);

    if (withTransaction) {
      JPA.runInTransaction(super::run);
    } else {
      super.run();
    }
  }
}
