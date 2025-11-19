/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.file.store.s3;

import com.axelor.common.FileUtils;
import com.axelor.common.MimeTypesUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.tenants.TenantConfig;
import com.axelor.db.tenants.TenantResolver;
import com.axelor.file.store.Store;
import com.axelor.file.store.StoreType;
import com.axelor.file.store.UploadedFile;
import com.axelor.file.temp.TempFiles;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.ServerSideEncryption;
import io.minio.ServerSideEncryptionKms;
import io.minio.ServerSideEncryptionS3;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class S3Store implements Store {

  private final S3ClientManager _s3ClientManager;
  private final S3Cache _s3Cache;

  // Define Part Size: 30 MB
  private static final long PART_SIZE = 30L * 1024 * 1024;

  public S3Store(S3ClientManager s3ClientManager) {
    this._s3ClientManager = s3ClientManager;

    try {
      createBucket();
    } catch (InternalException
        | ServerException
        | InsufficientDataException
        | ErrorResponseException
        | IOException
        | NoSuchAlgorithmException
        | InvalidKeyException
        | InvalidResponseException
        | XmlParserException e) {
      throw new RuntimeException(e);
    }

    this._s3Cache = S3Cache.getInstance();
  }

  private void createBucket()
      throws ServerException,
          InsufficientDataException,
          ErrorResponseException,
          IOException,
          NoSuchAlgorithmException,
          InvalidKeyException,
          InvalidResponseException,
          XmlParserException,
          InternalException {
    boolean isExist =
        getClient().bucketExists(BucketExistsArgs.builder().bucket(getBucketName()).build());
    if (!isExist) {
      getClient().makeBucket(MakeBucketArgs.builder().bucket(getBucketName()).build());
    }
  }

  public MinioClient getClient() {
    return _s3ClientManager.getClient();
  }

  public String getBucketName() {
    return _s3ClientManager.getBucketName();
  }

  private String getRootPath() {
    final String tenantId = TenantResolver.currentTenantIdentifier();
    if (StringUtils.isBlank(tenantId) || TenantConfig.DEFAULT_TENANT_ID.equals(tenantId)) {
      return "";
    }
    return tenantId + "/";
  }

  private String getObjectName(String fileName) {
    return getRootPath() + fileName;
  }

  @Override
  public boolean hasFile(String fileName) {
    boolean found = false;
    final String objectName = getObjectName(fileName);
    try {
      getClient()
          .statObject(StatObjectArgs.builder().bucket(getBucketName()).object(objectName).build());
      found = true;
    } catch (ErrorResponseException e) {
      String code = e.errorResponse().code();
      if (!("NoSuchKey".equals(code) || "ResourceNotFound".equals(code))) {
        throw new RuntimeException(e);
      }
    } catch (XmlParserException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | IOException
        | NoSuchAlgorithmException
        | ServerException e) {
      throw new RuntimeException(e);
    }
    return found;
  }

  @Override
  public UploadedFile addFile(InputStream inputStream, String fileName) {
    // We wrap the stream here to allow MimeTypesUtils to inspect bytes safely
    inputStream = inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream);

    try (InputStream safeStream = inputStream) {
      return uploadData(
          safeStream, fileName, -1, MimeTypesUtils.getContentType(safeStream, fileName));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public UploadedFile addFile(Path path, String fileName) {
    try (InputStream inputStream = Files.newInputStream(path)) {
      return uploadData(
          inputStream,
          fileName,
          Files.size(path),
          MimeTypesUtils.getContentType(path.toFile(), fileName));
    } catch (IOException
        | ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException e) {
      throw new RuntimeException(e);
    }
  }

  private UploadedFile uploadData(
      InputStream inputStream, String fileName, long fileSize, String contentType)
      throws NoSuchAlgorithmException,
          IOException,
          ServerException,
          InsufficientDataException,
          ErrorResponseException,
          InvalidKeyException,
          InvalidResponseException,
          XmlParserException,
          InternalException {
    Map<String, String> headers = new HashMap<>();
    if (StringUtils.notBlank(_s3ClientManager.getStorageClass())) {
      headers.put("X-Amz-Storage-Class", _s3ClientManager.getStorageClass());
    }

    final String objectName = getObjectName(fileName);
    PutObjectArgs.Builder builder =
        PutObjectArgs.builder()
            .bucket(getBucketName())
            .object(objectName)
            .contentType(contentType)
            .stream(inputStream, fileSize > 0 ? fileSize : -1, PART_SIZE)
            .headers(headers)
            .sse(getEncryption());
    getClient().putObject(builder.build());

    // Get actual size from S3 if it was unknown
    if (fileSize < 0) {
      StatObjectResponse stat =
          getClient()
              .statObject(
                  StatObjectArgs.builder().bucket(getBucketName()).object(objectName).build());
      fileSize = stat.size();
    }

    return new UploadedFile(
        FileUtils.getFileName(fileName), fileName, fileSize, contentType, getStoreType());
  }

  @Override
  public void deleteFile(String fileName) {
    final String objectName = getObjectName(fileName);
    try {
      RemoveObjectArgs.Builder builder =
          RemoveObjectArgs.builder().bucket(getBucketName()).object(objectName);
      getClient().removeObject(builder.build());
      if (S3Cache.CACHE_ENABLED) {
        _s3Cache.remove(fileName);
      }
    } catch (IOException
        | ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Path getPath(String fileName, boolean cache) {
    Path cachePath = S3Cache.CACHE_ENABLED && cache ? _s3Cache.get(fileName) : null;
    if (cachePath != null) {
      // if in cache, return it as a new temp file
      try {
        Path tempFile = TempFiles.createTempFile();
        FileUtils.copyPath(cachePath, tempFile);
        return tempFile;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    // fetch stream from S3
    try (InputStream inputStream = _fetchStream(fileName)) {
      // create tmp file from stream
      Path tempFile = TempFiles.createTempFile();
      FileUtils.write(tempFile, inputStream);
      if (S3Cache.CACHE_ENABLED && cache) {
        // put in the cache
        _s3Cache.put(tempFile.toFile(), fileName);
      }
      return tempFile;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public InputStream getStream(String fileName, boolean cache) {
    if (S3Cache.CACHE_ENABLED && cache) {
      try {
        // if in cache, return it
        Path cachePath = _s3Cache.get(fileName);
        if (cachePath != null) {
          return Files.newInputStream(cachePath);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    // fetch stream from S3
    InputStream inputStream = _fetchStream(fileName);

    if (S3Cache.CACHE_ENABLED && cache) {
      try (inputStream) {
        File cachedFile = _s3Cache.put(inputStream, fileName);
        return Files.newInputStream(cachedFile.toPath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return inputStream;
  }

  private InputStream _fetchStream(String fileName) {
    final String objectName = getObjectName(fileName);
    try {
      GetObjectArgs.Builder builder =
          GetObjectArgs.builder().bucket(getBucketName()).object(objectName);
      return getClient().getObject(builder.build());
    } catch (XmlParserException
        | ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | IOException
        | NoSuchAlgorithmException
        | ServerException e) {
      throw new RuntimeException(e);
    }
  }

  private ServerSideEncryption getEncryption()
      throws NoSuchAlgorithmException, JsonProcessingException {
    ServerSideEncryption sse = null;
    if (_s3ClientManager.getEncryptionType() == S3EncryptionType.SSE_S3) {
      sse = new ServerSideEncryptionS3();
    } else if (_s3ClientManager.getEncryptionType() == S3EncryptionType.SSE_KMS) {
      sse = new ServerSideEncryptionKms(_s3ClientManager.getEncryptionKmsKeyId(), null);
    }
    return sse;
  }

  @Override
  public StoreType getStoreType() {
    return StoreType.OBJECT_STORAGE;
  }

  @Override
  public void shutdown() {
    _s3ClientManager.shutdown();
  }
}
