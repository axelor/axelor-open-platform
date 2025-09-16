/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
