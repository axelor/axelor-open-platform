/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class S3Client {

  private static final long DEFAULT_CONNECTION_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

  private final S3Configuration configuration;

  private OkHttpClient okHttpClient;
  private MinioClient minioClient;

  private static final String SCHEME_SEPARATOR = "://";

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
      Provider iamAwsProvider = new IamAwsProvider(getIamAwsCustomEndpoint(), okHttpClient);
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

    if (configuration.isPathStyle()) {
      this.minioClient.disableVirtualStyleEndpoint();
    }

    return this;
  }

  private HttpUrl getEndpoint() {
    String endpoint = addScheme(configuration.getEndpoint());
    Objects.requireNonNull(endpoint, "Endpoint is required");
    return HttpUrl.get(endpoint);
  }

  private String getIamAwsCustomEndpoint() {
    return addScheme(configuration.getIamAwsCustomEndpoint());
  }

  private String addScheme(String endpoint) {
    if (endpoint == null || endpoint.contains(SCHEME_SEPARATOR)) {
      return endpoint;
    }

    String scheme = configuration.isSecure() ? "https" : "http";
    return scheme + SCHEME_SEPARATOR + endpoint;
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
