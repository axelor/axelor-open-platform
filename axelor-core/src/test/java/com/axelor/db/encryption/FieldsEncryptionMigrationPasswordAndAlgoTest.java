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
 * Verifies the combined password-and-algorithm migration scenario: fields encrypted with an old
 * password and CBC algorithm are re-encrypted with the new password and GCM algorithm in a single
 * migration pass.
 */
@GuiceModules(FieldsEncryptionMigrationPasswordAndAlgoTest.MigrationTestModule.class)
public class FieldsEncryptionMigrationPasswordAndAlgoTest extends BaseEncryptionMigrationTest {

  private static final String OLD_PASSWORD = "old-password";

  public static class MigrationTestModule extends AuditTestModule {
    @Override
    protected void configure() {
      AppSettings.get()
          .getInternalProperties()
          .put(AvailableAppSettings.ENCRYPTION_OLD_ALGORITHM, "CBC");
      AppSettings.get()
          .getInternalProperties()
          .put(AvailableAppSettings.ENCRYPTION_OLD_PASSWORD, OLD_PASSWORD);
      super.configure();
    }
  }

  @BeforeAll
  public static void setup() {
    insertData(StringEncryptor.cbc(OLD_PASSWORD), BytesEncryptor.cbc(OLD_PASSWORD));
  }
}
