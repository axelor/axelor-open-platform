/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.file.store;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.file.store.file.FileSystemStore;
import com.axelor.file.store.s3.DefaultS3ClientManager;
import com.axelor.file.store.s3.S3Store;
import jakarta.inject.Singleton;

@Singleton
public class FileStoreFactory {
  private static Store _store;

  public static Store getStore() {
    if (_store != null) {
      return _store;
    }

    boolean isObjectStorage =
        AppSettings.get().getBoolean(AvailableAppSettings.DATA_OBJECT_STORAGE_ENABLED, false);
    if (isObjectStorage) {
      _store = new S3Store(DefaultS3ClientManager.getInstance());
    } else {
      _store = new FileSystemStore();
    }

    return _store;
  }

  public static void shutdown() {
    if (_store != null) {
      _store.shutdown();
    }
  }
}
