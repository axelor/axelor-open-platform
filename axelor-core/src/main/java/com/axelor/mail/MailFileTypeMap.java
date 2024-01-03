/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.mail;

import com.axelor.common.MimeTypesUtils;
import java.io.File;
import javax.activation.FileTypeMap;

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
