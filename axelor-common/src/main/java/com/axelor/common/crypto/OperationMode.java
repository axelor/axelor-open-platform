/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

import javax.crypto.Cipher;

/** {@link Cipher} operation modes. */
public enum OperationMode {
  CBC,

  GCM
}
