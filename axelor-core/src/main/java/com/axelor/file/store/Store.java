/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

public interface Store {

  /**
   * Check if the file exist
   *
   * @param fileName the file's name
   * @return true if the file exists; false otherwise
   */
  boolean hasFile(String fileName);

  UploadedFile addFile(InputStream inputStream, String fileName);

  UploadedFile addFile(File file, String fileName);

  UploadedFile addFile(Path path, String fileName);

  void deleteFile(String fileName);

  File getFile(String fileName);

  File getFile(String fileName, boolean cache);

  InputStream getStream(String fileName);

  InputStream getStream(String fileName, boolean cache);

  StoreType getStoreType();

  void shutdown();
}
