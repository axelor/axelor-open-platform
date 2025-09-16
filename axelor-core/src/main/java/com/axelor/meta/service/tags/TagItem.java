/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.service.tags;

import com.google.common.base.MoreObjects;

public class TagItem {

  private String name;
  private String value;
  private String style;

  public TagItem(String name, String value, String style) {
    this.name = name;
    this.value = value;
    this.style = style;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getStyle() {
    return style;
  }

  public void setStyle(String style) {
    this.style = style;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("value", value)
        .add("style", style)
        .toString();
  }
}
