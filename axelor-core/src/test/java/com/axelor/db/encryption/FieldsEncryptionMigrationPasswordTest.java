/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.encryption;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.crypto.BytesEncryptor;
import com.axelor.common.crypto.StringEncryptor;
import com.axelor.test.GuiceModules;
import org.junit.jupiter.api.BeforeAll;

/**
 * Verifies the password-rotation migration scenario: fields encrypted with an old password are
 * re-encrypted with the new password during migration. Both algorithms remain GCM.
 */
@GuiceModules(FieldsEncryptionMigrationPasswordTest.MigrationTestModule.class)
public class FieldsEncryptionMigrationPasswordTest extends BaseEncryptionMigrationTest {

  private static final String OLD_PASSWORD = "old-password";

  public static class MigrationTestModule extends AuditTestModule {
    @Override
    protected void configure() {
      AppSettings.get()
          .getInternalProperties()
          .put(AvailableAppSettings.ENCRYPTION_OLD_PASSWORD, OLD_PASSWORD);
      super.configure();
    }
  }

  @BeforeAll
  public static void setup() {
    insertData(StringEncryptor.gcm(OLD_PASSWORD), BytesEncryptor.gcm(OLD_PASSWORD));
  }
}
