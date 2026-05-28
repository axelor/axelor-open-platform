/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.file.store;

/** Represents the type of storage used by a {@link Store} implementation. */
public enum StoreType {

  /** Represents a local file system–based storage (e.g., files stored on disk). */
  FILE_SYSTEM(1),

  /** Represents an object storage–based system (e.g., s3, etc.). */
  OBJECT_STORAGE(2);

  private final int value;

  /**
   * Constructs a new {@code StoreType} with the specified integer value.
   *
   * @param value the integer value representing the store type
   */
  private StoreType(int value) {
    this.value = value;
  }

  /**
   * Retrieves the integer value associated with this store type
   *
   * @return the integer value of the store type
   */
  public int getValue() {
    return value;
  }
}
