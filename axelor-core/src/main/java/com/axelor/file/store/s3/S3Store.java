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
package com.axelor.file.store.s3;

import com.axelor.common.FileUtils;
import com.axelor.common.MimeTypesUtils;
import com.axelor.common.StringUtils;
import com.axelor.file.store.Store;
import com.axelor.file.store.StoreType;
import com.axelor.file.store.UploadedFile;
import com.axelor.file.temp.TempFiles;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.ServerSideEncryption;
import io.minio.ServerSideEncryptionKms;
import io.minio.ServerSideEncryptionS3;
import io.minio.StatObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class S3Store implements Store {

  private final S3ClientManager _s3ClientManager;
  private final S3Cache _s3Cache;

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

  @Override
  public boolean hasFile(String fileName) {
    boolean found = false;
    try {
      getClient()
          .statObject(StatObjectArgs.builder().bucket(getBucketName()).object(fileName).build());
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
    Path tempFile = null;
    try {
      tempFile = TempFiles.createTempFile();
      FileUtils.write(tempFile, inputStream);
      return addFile(tempFile, fileName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        // ignore
      }
    }
  }

  @Override
  public UploadedFile addFile(File file, String fileName) {
    return addFile(file.toPath(), fileName);
  }

  @Override
  public UploadedFile addFile(Path path, String fileName) {
    if (hasFile(fileName)) {
      deleteFile(fileName);
    }
    try {
      String contentType = MimeTypesUtils.getContentType(path);
      Map<String, String> headers = new HashMap<>();
      if (StringUtils.notBlank(_s3ClientManager.getStorageClass())) {
        headers.put("X-Amz-Storage-Class", _s3ClientManager.getStorageClass());
      }
      UploadObjectArgs.Builder builder =
          UploadObjectArgs.builder()
              .bucket(getBucketName())
              .object(fileName)
              .contentType(contentType)
              .filename(path.toString())
              .headers(headers)
              .sse(getEncryption());
      getClient().uploadObject(builder.build());
      return new UploadedFile(
          FileUtils.getFileName(fileName), fileName, Files.size(path), contentType, getStoreType());
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
  public void deleteFile(String fileName) {
    try {
      RemoveObjectArgs.Builder builder =
          RemoveObjectArgs.builder().bucket(getBucketName()).object(fileName);
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
  public File getFile(String fileName) {
    return getFile(fileName, false);
  }

  @Override
  public File getFile(String fileName, boolean cache) {
    File cacheFile = S3Cache.CACHE_ENABLED ? _s3Cache.get(fileName) : null;
    if (cacheFile != null) {
      // if in cache, return it
      try {
        Path tempFile = TempFiles.createTempFile();
        FileUtils.copyFile(cacheFile, tempFile.toFile());
        return tempFile.toFile();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    // fetch stream from s3
    try (InputStream inputStream = _fetchStream(fileName)) {
      // create tmp file from stream
      Path tempFile = TempFiles.createTempFile();
      FileUtils.write(tempFile, inputStream);
      if (S3Cache.CACHE_ENABLED && cache) {
        // put in the cache
        _s3Cache.put(tempFile.toFile(), fileName);
      }
      return tempFile.toFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InputStream getStream(String fileName) {
    return getStream(fileName, false);
  }

  @Override
  public InputStream getStream(String fileName, boolean cache) {
    if (S3Cache.CACHE_ENABLED) {
      try {
        // if in cache, return it
        File cacheFile = _s3Cache.get(fileName);
        if (cacheFile != null) {
          return new FileInputStream(cacheFile);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    // fetch stream from s3
    InputStream inputStream = _fetchStream(fileName);

    if (S3Cache.CACHE_ENABLED && cache) {
      // put in the cache
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try {
        inputStream.transferTo(baos);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      _s3Cache.put(new ByteArrayInputStream(baos.toByteArray()), fileName);
      return new ByteArrayInputStream(baos.toByteArray());
    }

    return inputStream;
  }

  private InputStream _fetchStream(String fileName) {
    try {
      GetObjectArgs.Builder builder =
          GetObjectArgs.builder().bucket(getBucketName()).object(fileName);
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
