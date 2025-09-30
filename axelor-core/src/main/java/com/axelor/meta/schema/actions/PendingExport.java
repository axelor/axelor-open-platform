/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.actions;

import java.io.InputStream;

/**
 * Represents a pending export operation with its associated input stream and file name.
 *
 * @param stream the input stream containing the export data, must not be null
 * @param name the name of the export file
 */
public record PendingExport(InputStream stream, String name) {

  /**
   * Compact constructor with validation.
   *
   * @throws IllegalArgumentException if stream is null
   */
  public PendingExport {
    if (stream == null) {
      throw new IllegalArgumentException("Export stream cannot be null");
    }
  }
}
