/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2026 Axelor (<http://axelor.com>).
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
