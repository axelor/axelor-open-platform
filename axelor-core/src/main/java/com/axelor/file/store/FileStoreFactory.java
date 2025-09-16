/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
