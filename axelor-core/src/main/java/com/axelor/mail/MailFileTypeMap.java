/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail;

import com.axelor.common.MimeTypesUtils;
import jakarta.activation.FileTypeMap;
import java.io.File;

/**
 * Custom {@link FileTypeMap} implementation.
 *
 * <p>This class uses {@link MimeTypesUtils#getContentType(File)} to find file type and
 */
class MailFileTypeMap extends FileTypeMap {

  @Override
  public String getContentType(File file) {
    return MimeTypesUtils.getContentType(file);
  }

  @Override
  public String getContentType(String filename) {
    return MimeTypesUtils.getContentType(filename);
  }
}
