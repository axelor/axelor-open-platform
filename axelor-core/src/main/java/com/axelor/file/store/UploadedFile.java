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
package com.axelor.file.store;

import com.google.common.base.MoreObjects;

public class UploadedFile {

  private String name;
  private String path;
  private long size;
  private String contentType;
  private StoreType storeType;

  public UploadedFile(
      String name, String path, long size, String contentType, StoreType storeType) {
    this.name = name;
    this.path = path;
    this.size = size;
    this.contentType = contentType;
    this.storeType = storeType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public StoreType getStoreType() {
    return storeType;
  }

  public void setStoreType(StoreType storeType) {
    this.storeType = storeType;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("path", path)
        .add("size", size)
        .add("contentType", contentType)
        .add("storeType", storeType)
        .toString();
  }
}
