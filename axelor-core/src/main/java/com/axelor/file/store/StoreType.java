/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.file.store;

public enum StoreType {
  FILE_SYSTEM(1),
  OBJECT_STORAGE(2);

  private final int value;

  private StoreType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
