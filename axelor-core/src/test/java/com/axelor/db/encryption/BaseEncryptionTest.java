/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.encryption;

import com.axelor.JpaTest;
import com.axelor.JpaTestModule;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.crypto.BytesEncryptor;
import com.axelor.common.crypto.StringEncryptor;
import com.axelor.test.GuiceModules;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

@GuiceModules(BaseEncryptionTest.AuditTestModule.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
abstract class BaseEncryptionTest extends JpaTest {

  protected static String ENCRYPTION_ALGORITHM = "GCM";
  protected static String ENCRYPTION_PASSWORD = "123456789";

  public static class AuditTestModule extends JpaTestModule {
    @Override
    protected void configure() {
      AppSettings.get()
          .getInternalProperties()
          .put(AvailableAppSettings.ENCRYPTION_ALGORITHM, ENCRYPTION_ALGORITHM);

      AppSettings.get()
          .getInternalProperties()
          .put(AvailableAppSettings.ENCRYPTION_PASSWORD, ENCRYPTION_PASSWORD);
      super.configure();
    }
  }

  @AfterAll
  public static void cleanup() {
    AppSettings.get().getInternalProperties().remove(AvailableAppSettings.ENCRYPTION_ALGORITHM);
    AppSettings.get().getInternalProperties().remove(AvailableAppSettings.ENCRYPTION_PASSWORD);
    AppSettings.get().getInternalProperties().remove(AvailableAppSettings.ENCRYPTION_OLD_ALGORITHM);
    AppSettings.get().getInternalProperties().remove(AvailableAppSettings.ENCRYPTION_OLD_PASSWORD);
  }

  protected StringEncryptor getStringEncryptor() {
    return "GCM".equalsIgnoreCase(ENCRYPTION_ALGORITHM)
        ? StringEncryptor.gcm(ENCRYPTION_PASSWORD)
        : StringEncryptor.cbc(ENCRYPTION_PASSWORD);
  }

  protected BytesEncryptor getBytesEncryptor() {
    return "GCM".equalsIgnoreCase(ENCRYPTION_ALGORITHM)
        ? BytesEncryptor.gcm(ENCRYPTION_PASSWORD)
        : BytesEncryptor.cbc(ENCRYPTION_PASSWORD);
  }
}
