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
      AppSettings.get()
          .getInternalProperties()
          .put(AvailableAppSettings.ENCRYPTION_OLD_ALGORITHM, "GCM");
      super.configure();
    }
  }

  @BeforeAll
  public static void setup() {
    insertData(StringEncryptor.gcm(OLD_PASSWORD), BytesEncryptor.gcm(OLD_PASSWORD));
  }
}
