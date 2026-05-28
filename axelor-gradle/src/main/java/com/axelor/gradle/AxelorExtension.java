/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle;

public class AxelorExtension {

  public static final String EXTENSION_NAME = "axelor";

  private String title;

  private String description;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void title(String title) {
    this.title = title;
  }

  public void description(String description) {
    this.description = description;
  }
}
