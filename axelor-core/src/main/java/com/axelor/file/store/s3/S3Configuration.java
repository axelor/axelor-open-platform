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
import com.google.common.base.MoreObjects;

public class S3Configuration {

  public String endpoint;
  public boolean pathStyle = false;

  public boolean secure = true;

  public String accessKey;
  public String secretKey;

  public String bucket;

  public String region;

  public S3Configuration(
      String endpoint,
      boolean pathStyle,
      boolean secure,
      String accessKey,
      String secretKey,
      String bucket,
      String region) {
    this.endpoint = endpoint;
    this.pathStyle = pathStyle;
    this.secure = secure;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.bucket = bucket;
    this.region = region;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public boolean isPathStyle() {
    return pathStyle;
  }

  public void setPathStyle(boolean pathStyle) {
    this.pathStyle = pathStyle;
  }

  public boolean isSecure() {
    return secure;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  @Override
  public String toString() {
    MoreObjects.ToStringHelper helper =
        MoreObjects.toStringHelper(this)
            .add("endpoint", endpoint)
            .add("pathStyle", pathStyle)
            .add("secure", secure)
            .add("bucket", bucket)
            .add("region", region);
    if (StringUtils.notEmpty(accessKey)) {
      helper.add("accessKey", "******");
    }
    if (StringUtils.notEmpty(secretKey)) {
      helper.add("secretKey", "******");
    }
    return helper.toString();
  }
}
