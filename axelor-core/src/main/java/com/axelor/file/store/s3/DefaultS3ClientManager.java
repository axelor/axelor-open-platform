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
package com.axelor.file.store.s3;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.StringUtils;
import io.minio.MinioClient;
import jakarta.inject.Singleton;
import okhttp3.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultS3ClientManager implements S3ClientManager {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultS3ClientManager.class);

  private static volatile DefaultS3ClientManager instance;

  private final S3Client s3Client;
  private final S3Configuration s3Configuration;

  private DefaultS3ClientManager() {
    // protect against instantiation via reflection
    if (instance != null) {
      throw new RuntimeException(
          "Use getInstance() method to get the single instance of this class.");
    }

    s3Configuration = getDefaultConfiguration();
    s3Client = new S3Client(s3Configuration).build();
  }

  private S3Configuration getDefaultConfiguration() {
    final AppSettings settings = AppSettings.get();
    return new S3Configuration(
        settings.get(AvailableAppSettings.DATA_OBJECT_STORAGE_ENDPOINT),
        settings.getBoolean(AvailableAppSettings.DATA_OBJECT_STORAGE_PATH_STYLE, false),
        settings.getBoolean(AvailableAppSettings.DATA_OBJECT_STORAGE_SECURE, true),
        settings.get(AvailableAppSettings.DATA_OBJECT_STORAGE_ACCESS_KEY),
        settings.get(AvailableAppSettings.DATA_OBJECT_STORAGE_SECRET_KEY),
        settings.get(AvailableAppSettings.DATA_OBJECT_STORAGE_BUCKET),
        settings.get(AvailableAppSettings.DATA_OBJECT_STORAGE_REGION),
        getEncryption(settings.get(AvailableAppSettings.DATA_OBJECT_STORAGE_ENCRYPTION)),
        settings.get(AvailableAppSettings.DATA_OBJECT_STORAGE_ENCRYPTION_KMS_KEY_ID));
  }

  private S3EncryptionType getEncryption(String encryptionName) {
    if (StringUtils.isEmpty(encryptionName)) {
      return null;
    }
    return S3EncryptionType.from(encryptionName);
  }

  public static DefaultS3ClientManager getInstance() {
    if (instance == null) {
      synchronized (DefaultS3ClientManager.class) {
        if (instance == null) instance = new DefaultS3ClientManager();
      }
    }

    return instance;
  }

  @Override
  public void shutdown() {
    try {
      Cache cache = s3Client.getOkHttpClient().cache();
      if (cache != null) {
        cache.close();
      }
      s3Client.getOkHttpClient().dispatcher().executorService().shutdown();
      s3Client.getOkHttpClient().connectionPool().evictAll();
    } catch (Exception e) {
      LOG.error("Unable to shutdown s3 connections", e);
    }
  }

  @Override
  public MinioClient getClient() {
    return s3Client.getMinioClient();
  }

  @Override
  public String getBucketName() {
    return s3Configuration.getBucket();
  }

  @Override
  public S3EncryptionType getEncryptionType() {
    return s3Configuration.getEncryption();
  }

  @Override
  public String getEncryptionKmsKeyId() {
    return s3Configuration.getKmsKeyId();
  }
}
