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
package com.axelor.file.store.file;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.FileUtils;
import com.axelor.common.MimeTypesUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.tenants.TenantResolver;
import com.axelor.file.store.Store;
import com.axelor.file.store.StoreType;
import com.axelor.file.store.UploadedFile;
import com.axelor.file.temp.TempFiles;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileSystemStore implements Store {

  private static final String DEFAULT_UPLOAD_PATH = "{java.io.tmpdir}/.axelor";

  private static final Path UPLOAD_PATH =
      Paths.get(AppSettings.get().get(AvailableAppSettings.DATA_UPLOAD_DIR, DEFAULT_UPLOAD_PATH));

  private static final CopyOption[] COPY_OPTIONS = {
    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES
  };

  private static final CopyOption[] MOVE_OPTIONS = {StandardCopyOption.REPLACE_EXISTING};

  public FileSystemStore() {
    try {
      Files.createDirectories(getRootPath());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public boolean hasFile(String fileName) {
    return Files.exists(resolveFilePath(fileName));
  }

  private Path getRootPath() {
    String tenantId = TenantResolver.currentTenantIdentifier();
    if (StringUtils.isBlank(tenantId)) {
      return UPLOAD_PATH;
    }
    return UPLOAD_PATH.resolve(tenantId);
  }

  private Path resolveFilePath(String fileName) {
    // Make sure the file is located in the upload directory
    if (!FileUtils.isChildPath(getRootPath(), Paths.get(fileName))) {
      throw new IllegalArgumentException(
          "Invalid file name: " + fileName + ". Not located in the upload directory.");
    }
    return getRootPath().resolve(fileName);
  }

  @Override
  public UploadedFile addFile(InputStream inputStream, String fileName) {
    try {
      Path targetFile = resolveFilePath(fileName);
      FileUtils.write(targetFile, inputStream);
      return new UploadedFile(
          FileUtils.getFileName(targetFile),
          getRootPath().relativize(targetFile).toString(),
          Files.size(targetFile),
          MimeTypesUtils.getContentType(targetFile),
          getStoreType());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public UploadedFile addFile(File file, String fileName) {
    return addFile(file.toPath(), fileName);
  }

  @Override
  public UploadedFile addFile(Path path, String fileName) {
    Path targetFile = resolveFilePath(fileName);
    try {
      if (TempFiles.getTempPath().equals(path.getParent())) {
        Files.move(path, targetFile, MOVE_OPTIONS);
      } else {
        Files.copy(path, targetFile, COPY_OPTIONS);
      }
      return new UploadedFile(
          FileUtils.getFileName(targetFile),
          getRootPath().relativize(targetFile).toString(),
          Files.size(targetFile),
          MimeTypesUtils.getContentType(targetFile),
          getStoreType());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void deleteFile(String fileName) {
    try {
      Files.deleteIfExists(resolveFilePath(fileName));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public File getFile(String fileName) {
    File file = resolveFilePath(fileName).toFile();
    if (file.exists()) {
      return file;
    }
    throw new RuntimeException("The file doesn't exist: " + fileName);
  }

  @Override
  public File getFile(String fileName, boolean cache) {
    return getFile(fileName);
  }

  @Override
  public InputStream getStream(String fileName) {
    try {
      return new FileInputStream(resolveFilePath(fileName).toFile());
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InputStream getStream(String fileName, boolean cache) {
    return getStream(fileName);
  }

  @Override
  public StoreType getStoreType() {
    return StoreType.FILE_SYSTEM;
  }

  @Override
  public void shutdown() {
    // nothing to do
  }
}
