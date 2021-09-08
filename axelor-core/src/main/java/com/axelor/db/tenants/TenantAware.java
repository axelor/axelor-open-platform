/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
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
package com.axelor.db.tenants;

import com.axelor.db.JPA;

public class TenantAware implements Runnable {

  private String tenantId;

  private String tenantHost;

  private boolean transactional;

  private Runnable task;

  public TenantAware() {}

  public TenantAware tenantId(String tenantId) {
    this.tenantId = tenantId;
    return this;
  }

  public TenantAware tenantHost(String tenantHost) {
    this.tenantHost = tenantHost;
    return this;
  }

  public TenantAware transactional() {
    this.transactional = true;
    return this;
  }

  public TenantAware task(Runnable task) {
    this.task = task;
    return this;
  }

  @Override
  public void run() {
    String currentId = TenantResolver.CURRENT_TENANT.get();
    String currentHost = TenantResolver.CURRENT_HOST.get();
    TenantResolver.CURRENT_TENANT.set(tenantId);
    TenantResolver.CURRENT_HOST.set(tenantHost);
    try {
      if (transactional) {
        JPA.runInTransaction(task);
      } else {
        task.run();
      }
    } finally {
      TenantResolver.CURRENT_TENANT.set(currentId);
      TenantResolver.CURRENT_HOST.set(currentHost);
    }
  }
}
