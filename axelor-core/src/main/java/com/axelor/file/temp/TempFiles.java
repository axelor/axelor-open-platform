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
package com.axelor.file.temp;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.stream.Stream;

public class TempFiles {

  private static final String DEFAULT_UPLOAD_TEMP_PATH = "{java.io.tmpdir}/.axelor";
  private static final String TEMP_DIR_NAME = "tmp_files";

  private static final Path TMP_PATH =
      Paths.get(
          AppSettings.get()
              .get(AvailableAppSettings.DATA_UPLOAD_TEMP_DIR, DEFAULT_UPLOAD_TEMP_PATH));

  // temp clean up threshold 24 hours
  private static final long TEMP_THRESHOLD = 24 * 3600 * 1000;

  /**
   * Get the path to deal with temp upload file/dir
   *
   * @return the temp path
   */
  public static Path getTempPath() {
    return getRootTempPath().resolve(TEMP_DIR_NAME);
  }

  /**
   * Get the root path to deal with temp file/dir
   *
   * @return the root temp path
   */
  public static Path getRootTempPath() {
    return TMP_PATH;
  }

  /**
   * Create a temporary file.
   *
   * @return the path to the newly created file
   * @throws IOException if an I/O error occurs
   */
  public static Path createTempFile() throws IOException {
    return createTempFile(null, null);
  }

  /**
   * Create a temporary file.
   *
   * @param prefix the file prefix to use
   * @param suffix the file suffix to use
   * @param attrs an optional list of file attributes
   * @return the path to the newly created file
   * @throws IOException if an I/O error occurs
   * @see Files#createTempFile(String, String, FileAttribute...)
   */
  public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs)
      throws IOException {
    Path tmp = getTempPath();
    Files.createDirectories(tmp);
    return Files.createTempFile(tmp, prefix, suffix, attrs);
  }

  /**
   * Find a temporary file by the given name created previously.
   *
   * @param name name of the temp file
   * @return file path
   */
  public static Path findTempFile(String name) {
    return getTempPath().resolve(name);
  }

  /**
   * Create a temporary directory
   *
   * @return the path to the newly created directory
   * @throws IOException if an I/O error occurs
   */
  public static Path createTempDir() throws IOException {
    return createTempDir(null);
  }

  /**
   * Create a temporary directory
   *
   * @param prefix the prefix string to be used in generating the directory's name
   * @return the path to the newly created directory
   * @throws IOException if an I/O error occurs
   */
  public static Path createTempDir(String prefix) throws IOException {
    Path tmp = getTempPath();
    Files.createDirectories(tmp);
    return Files.createTempDirectory(tmp, prefix);
  }

  /**
   * This method can be used to delete temporary file of an incomplete upload.
   *
   * @param fileId the upload file id
   * @throws IOException if an I/O error occurs
   */
  public static void clean(String fileId) throws IOException {
    Files.deleteIfExists(findTempFile(fileId));
  }

  /**
   * Clean up obsolete temporary files from upload directory.
   *
   * @throws IOException if an I/O error occurs
   */
  public static void clean() throws IOException {
    Path tempPath = getTempPath();
    if (!Files.isDirectory(tempPath)) {
      return;
    }
    final long currentTime = System.currentTimeMillis();
    Files.walkFileTree(
        tempPath,
        new SimpleFileVisitor<Path>() {

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            long diff = currentTime - Files.getLastModifiedTime(file).toMillis();
            if (diff >= TEMP_THRESHOLD) {
              Files.deleteIfExists(file);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path directory, IOException ioException)
              throws IOException {

            if (tempPath.equals(directory)) {
              return FileVisitResult.CONTINUE;
            }

            try (Stream<Path> stream = Files.list(directory)) {
              if (!stream.iterator().hasNext()) {
                Files.delete(directory);
              }
            }

            return FileVisitResult.CONTINUE;
          }
        });
  }
}
