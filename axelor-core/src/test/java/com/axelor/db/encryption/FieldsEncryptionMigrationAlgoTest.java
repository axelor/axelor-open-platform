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
 * Verifies the algorithm-change migration scenario: fields encrypted with CBC are re-encrypted with
 * GCM during migration. The password remains unchanged.
 */
@GuiceModules(FieldsEncryptionMigrationAlgoTest.MigrationTestModule.class)
public class FieldsEncryptionMigrationAlgoTest extends BaseEncryptionMigrationTest {

  public static class MigrationTestModule extends AuditTestModule {
    @Override
    protected void configure() {
      AppSettings.get()
          .getInternalProperties()
          .put(AvailableAppSettings.ENCRYPTION_OLD_ALGORITHM, "CBC");
      super.configure();
    }
  }

  @BeforeAll
  public static void setup() {
    insertData(StringEncryptor.cbc(ENCRYPTION_PASSWORD), BytesEncryptor.cbc(ENCRYPTION_PASSWORD));
  }
}
