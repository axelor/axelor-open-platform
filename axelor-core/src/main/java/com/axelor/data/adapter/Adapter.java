/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.data.adapter;

import java.util.Map;
import java.util.Properties;

public abstract class Adapter {

  private Properties options;

  public void setOptions(Properties options) {
    this.options = options;
  }

  public String get(String option, String defaultValue) {
    if (options == null) {
      return defaultValue;
    }
    return options.getProperty(option, defaultValue);
  }

  public abstract Object adapt(Object value, Map<String, Object> context);
}
