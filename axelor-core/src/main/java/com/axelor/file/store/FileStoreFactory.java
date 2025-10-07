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

/**
 * Factory class for creating and managing the {@link Store} instance used by the application.
 *
 * <p>This factory provides a unified access point to obtain the appropriate {@link Store}
 * implementation based on the application configuration.
 *
 * <pre>{@code
 * Store store = FileStoreFactory.getStore();
 * store.addFile(inputStream, "example.txt");
 * }</pre>
 */
public class FileStoreFactory {

  private static volatile Store _store;

  private FileStoreFactory() {}

  /**
   * Retrieves the instance of the {@link Store} configured for the application.
   *
   * @return the active {@link Store} implementation
   */
  public static Store getStore() {
    Store s = _store;
    if (s == null) {
      synchronized (FileStoreFactory.class) {
        s = _store;
        if (s == null) {
          boolean isObjectStorage =
              AppSettings.get().getBoolean(AvailableAppSettings.DATA_OBJECT_STORAGE_ENABLED, false);
          if (isObjectStorage) {
            s = new S3Store(DefaultS3ClientManager.getInstance());
          } else {
            s = new FileSystemStore();
          }
          _store = s;
        }
      }
    }
    return s;
  }

  /**
   * Shuts down the active {@link Store} instance, if one has been created.
   *
   * <p>This allows for cleanup of any resources held by the underlying storage implementation.
   */
  public static void shutdown() {
    Store s = _store;
    if (s != null) {
      s.shutdown();
      _store = null;
    }
  }
}
