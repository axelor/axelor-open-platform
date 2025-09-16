/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.theme;

import com.axelor.meta.db.MetaTheme;

/** Represent a selectable theme */
public class AvailableTheme {

  private String name;
  private String title;

  public AvailableTheme(MetaTheme metaTheme) {
    this.name = metaTheme.getId().toString();
    this.title = metaTheme.getName();
  }

  public AvailableTheme(String name) {
    this.name = name;
    this.title = name;
  }

  public AvailableTheme(String name, String title) {
    this.name = name;
    this.title = title;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }
}
