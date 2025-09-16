/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.actions.validate.validator;

public enum ValidatorType {
  NOTIFY("notify"),

  INFO("info"),

  ALERT("alert"),

  ERROR("error");

  private final String key;

  ValidatorType(String key) {
    this.key = key;
  }

  @Override
  public String toString() {
    return key;
  }
}
