/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.file.store;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

public interface Store {

  /**
   * Check if the file exists
   *
   * @param fileName the file's name
   * @return true if the file exists; false otherwise
   */
  boolean hasFile(String fileName);

  UploadedFile addFile(InputStream inputStream, String fileName);

  UploadedFile addFile(Path path, String fileName);

  default UploadedFile addFile(File file, String fileName) {
    return addFile(file.toPath(), fileName);
  }

  void deleteFile(String fileName);

  default Path getPath(String fileName) {
    return getPath(fileName, false);
  }

  Path getPath(String fileName, boolean cache);

  default File getFile(String fileName) {
    return getPath(fileName).toFile();
  }

  default File getFile(String fileName, boolean cache) {
    return getPath(fileName, cache).toFile();
  }

  default InputStream getStream(String fileName) {
    return getStream(fileName, false);
  }

  InputStream getStream(String fileName, boolean cache);

  StoreType getStoreType();

  void shutdown();
}
