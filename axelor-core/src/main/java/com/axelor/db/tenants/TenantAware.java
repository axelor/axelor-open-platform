/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
    String currentId = TenantResolver.CURRENT_TENANT.get();
    String currentHost = TenantResolver.CURRENT_HOST.get();
    TenantResolver.setCurrentTenant(tenantId, tenantHost);

    String currentBaseUrl = AppFilter.getBaseURL();
    Locale currentLocale = AppFilter.getLanguage();
    AppFilter.setBaseURL(baseUrl);
    AppFilter.setLanguage(language);

    try {
      if (withTransaction) {
        JPA.runInTransaction(super::run);
      } else {
        super.run();
      }
    } finally {
      TenantResolver.setCurrentTenant(currentId, currentHost);

      AppFilter.setBaseURL(currentBaseUrl);
      AppFilter.setLanguage(currentLocale);
    }
  }
}
