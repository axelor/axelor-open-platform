/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.file.store.s3;

public enum S3EncryptionType {
  SSE_S3("SSE-S3"),
  SSE_KMS("SSE-KMS");

  private final String name;

  private S3EncryptionType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static S3EncryptionType from(String value) {
    for (S3EncryptionType type : values()) {
      if (type.getName().equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException(
        "Unable to get the correct S3 encryption type for '" + value + "'");
  }
}
