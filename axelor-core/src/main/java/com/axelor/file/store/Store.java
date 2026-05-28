/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.file.store;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

/** Represents an interface for managing file storage. */
public interface Store {

  /**
   * Checks if a file with the specified name exists in the store.
   *
   * @param fileName the name of the file to check for existence
   * @return true if a file with the specified name exists, false otherwise
   */
  boolean hasFile(String fileName);

  /**
   * Adds a file to the store using the provided input stream and file name.
   *
   * @param inputStream the input stream containing the file data
   * @param fileName the name of the file to be stored
   * @return an {@link UploadedFile} object representing the details of the uploaded file
   */
  UploadedFile addFile(InputStream inputStream, String fileName);

  /**
   * Adds a file to the store using the specified file path and file name.
   *
   * @param path the {@link Path} of the file to be added
   * @param fileName the name under which the file should be stored
   * @return an {@link UploadedFile} object containing the details of the uploaded file
   */
  UploadedFile addFile(Path path, String fileName);

  /**
   * Adds a file to the store using the specified file and file name.
   *
   * @param file the {@link File} object to be added to the store
   * @param fileName the name under which the file should be stored
   * @return an {@link UploadedFile} object containing the details of the uploaded file
   */
  default UploadedFile addFile(File file, String fileName) {
    return addFile(file.toPath(), fileName);
  }

  /**
   * Deletes a file with the specified name from the store.
   *
   * @param fileName the name of the file to be deleted
   */
  void deleteFile(String fileName);

  /**
   * Retrieves a {@link Path} representation of the specified file in the store.
   *
   * @param fileName the name of the file whose path is to be retrieved
   * @return the {@link Path} object representing the storage location of the file
   */
  default Path getPath(String fileName) {
    return getPath(fileName, false);
  }

  /**
   * Retrieves a {@link Path} representation of the specified file in the store.
   *
   * @param fileName the name of the file whose path is to be retrieved
   * @param cache whether to use cached storage for retrieving the file path
   * @return the {@link Path} object representing the storage location of the file
   */
  Path getPath(String fileName, boolean cache);

  /**
   * Retrieves a {@link File} representation of the specified file in the store.
   *
   * @param fileName the name of the file to retrieve
   * @return the {@link File} object representing the specified file, or null if the file does not
   *     exist
   */
  default File getFile(String fileName) {
    return getPath(fileName).toFile();
  }

  /**
   * Retrieves a {@link File} representation of the specified file in the store.
   *
   * @param fileName the name of the file to retrieve
   * @param cache whether to use cached storage for retrieving the file
   * @return the {@link File} object representing the specified file, or null if the file does not
   *     exist
   */
  default File getFile(String fileName, boolean cache) {
    return getPath(fileName, cache).toFile();
  }

  /**
   * Retrieves an input stream for the specified file.
   *
   * @param fileName the name of the file to retrieve the input stream for
   * @return an {@link InputStream} for the specified file
   */
  default InputStream getStream(String fileName) {
    return getStream(fileName, false);
  }

  /**
   * Retrieves an input stream for the specified file.
   *
   * @param fileName the name of the file to retrieve the input stream for
   * @param cache whether the file should be retrieved from a cached source
   * @return an {@link InputStream} for the specified file
   */
  InputStream getStream(String fileName, boolean cache);

  /**
   * Retrieves the type of store being used.
   *
   * @return the {@link StoreType} representing the type of storage
   */
  StoreType getStoreType();

  /**
   * Performs the necessary cleanup or resource release operations to gracefully shut down the
   * storage.
   */
  void shutdown();
}
