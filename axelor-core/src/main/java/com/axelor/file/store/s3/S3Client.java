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

import com.axelor.common.StringUtils;
import io.minio.MinioClient;
import okhttp3.HttpUrl;

public class S3Client {

  private final S3Configuration configuration;

  public S3Client(S3Configuration configuration) {
    this.configuration = configuration;
  }

  public MinioClient getClient() {
    MinioClient.Builder builder =
        MinioClient.builder()
            .endpoint(getEndpoint())
            .credentials(configuration.getAccessKey(), configuration.getSecretKey());

    if (StringUtils.notEmpty(configuration.getRegion())) {
      builder.region(configuration.getRegion());
    }

    return builder.build();
  }

  private HttpUrl getEndpoint() {
    String host = configuration.getEndpoint();
    if (!configuration.isPathStyle()) {
      host = configuration.getBucket() + "." + host;
    }
    String scheme = configuration.isSecure() ? "https" : "http";
    return HttpUrl.get(scheme + "://" + host);
  }
}
