/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.report.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.axelor.JpaTest;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.db.JPA;
import com.axelor.db.tenants.TenantResolver;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

class ReportExecutorTest extends JpaTest {

  @Test
  void testUnitOfWork() throws Exception {
    EntityManager first = ReportExecutor.submit(JPA::em).get();
    EntityManager second = ReportExecutor.submit(JPA::em).get();

    // each task gets its own entity manager, closed once the task is done
    assertNotSame(first, second);
    assertFalse(first.isOpen());
    assertFalse(second.isOpen());
  }

  @Test
  void testCallerTenant() throws Exception {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.CONFIG_MULTI_TENANCY, "true");
    new TenantResolver();
    try {
      TenantResolver.setCurrentTenant("tenant-b");
      assertEquals(
          "tenant-b", ReportExecutor.submit(TenantResolver::currentTenantIdentifier).get());
    } finally {
      TenantResolver.setCurrentTenant(null, null);
      AppSettings.get().getInternalProperties().remove(AvailableAppSettings.CONFIG_MULTI_TENANCY);
      new TenantResolver();
    }
  }
}
