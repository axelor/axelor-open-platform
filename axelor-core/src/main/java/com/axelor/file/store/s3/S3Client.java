/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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

import com.axelor.common.StringUtils;
import io.minio.MinioClient;
import io.minio.credentials.AwsConfigProvider;
import io.minio.credentials.AwsEnvironmentProvider;
import io.minio.credentials.ChainedProvider;
import io.minio.credentials.IamAwsProvider;
import io.minio.credentials.Provider;
import io.minio.http.HttpUtils;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class S3Client {

  private static final long DEFAULT_CONNECTION_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

  private final S3Configuration configuration;

  private OkHttpClient okHttpClient;
  private MinioClient minioClient;

  public S3Client(S3Configuration configuration) {
    this.configuration = configuration;
  }

  public S3Client build() {
    MinioClient.Builder builder = MinioClient.builder().endpoint(getEndpoint());

    this.okHttpClient =
        HttpUtils.newDefaultHttpClient(
            DEFAULT_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);

    if (StringUtils.notEmpty(configuration.getAccessKey())
        && StringUtils.notEmpty(configuration.getSecretKey())) {
      builder.credentials(configuration.getAccessKey(), configuration.getSecretKey());
    } else {
      Provider iamAwsProvider =
          new IamAwsProvider(configuration.getIamAwsCustomEndpoint(), okHttpClient);
      Provider awsConfigProvider =
          new AwsConfigProvider(
              configuration.getAwsConfigFilename(), configuration.getAwsConfigProfile());
      Provider awsEnvironmentProvider = new AwsEnvironmentProvider();
      builder.credentialsProvider(
          new ChainedProvider(iamAwsProvider, awsConfigProvider, awsEnvironmentProvider));
    }

    if (StringUtils.notEmpty(configuration.getRegion())) {
      builder.region(configuration.getRegion());
    }

    builder.httpClient(okHttpClient);

    this.minioClient = builder.build();

    return this;
  }

  private HttpUrl getEndpoint() {
    String host = configuration.getEndpoint();
    if (!configuration.isPathStyle()) {
      host = configuration.getBucket() + "." + host;
    }
    String scheme = configuration.isSecure() ? "https" : "http";
    return HttpUrl.get(scheme + "://" + host);
  }

  public void shutdown() throws IOException {
    if (okHttpClient != null) {
      var cache = okHttpClient.cache();
      if (cache != null) {
        cache.close();
      }
      okHttpClient.dispatcher().executorService().shutdown();
      okHttpClient.connectionPool().evictAll();
    }
  }

  public MinioClient getMinioClient() {
    return minioClient;
  }
}
