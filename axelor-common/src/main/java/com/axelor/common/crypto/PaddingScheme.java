/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

import javax.crypto.Cipher;

/** {@link Cipher} padding schemes. */
public enum PaddingScheme {
  NONE("NoPadding"),

  PKCS5("PKCS5Padding");

  private String name;

  private PaddingScheme(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
