/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.file.store.file;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.FileUtils;
import com.axelor.common.MimeTypesUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.tenants.TenantConfig;
import com.axelor.db.tenants.TenantResolver;
import com.axelor.file.store.Store;
import com.axelor.file.store.StoreType;
import com.axelor.file.store.UploadedFile;
import com.axelor.file.temp.TempFiles;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileSystemStore implements Store {

  private static final String DEFAULT_UPLOAD_PATH = "{java.io.tmpdir}/.axelor";

  private static final Path UPLOAD_PATH =
      Path.of(AppSettings.get().get(AvailableAppSettings.DATA_UPLOAD_DIR, DEFAULT_UPLOAD_PATH));

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
    if (StringUtils.isBlank(tenantId) || TenantConfig.DEFAULT_TENANT_ID.equals(tenantId)) {
      return UPLOAD_PATH;
    }
    return UPLOAD_PATH.resolve(tenantId);
  }

  private Path resolveFilePath(String fileName) {
    // Make sure the file is located in the upload directory
    if (!FileUtils.isChildPath(getRootPath(), Path.of(fileName))) {
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
  public Path getPath(String fileName, boolean cache) {
    Path path = resolveFilePath(fileName);
    if (Files.exists(path)) {
      return path;
    }
    throw new RuntimeException("The file doesn't exist: " + fileName);
  }

  @Override
  public InputStream getStream(String fileName, boolean cache) {
    try {
      return Files.newInputStream(resolveFilePath(fileName));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
