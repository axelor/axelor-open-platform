/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.file.store.s3;

import io.minio.MinioClient;

public interface S3ClientManager {

  MinioClient getClient();

  String getBucketName();

  S3EncryptionType getEncryptionType();

  String getEncryptionKmsKeyId();

  void shutdown();

  String getStorageClass();
}
