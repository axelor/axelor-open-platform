/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code;

import com.axelor.common.StringUtils;

public interface JavaCodeUtils {

  static String firstUpper(String name) {
    if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
      return name;
    }
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  static String firstLower(String string) {
    if (string.length() > 1 && Character.isUpperCase(string.charAt(1))) {
      return string;
    }
    return string.substring(0, 1).toLowerCase() + string.substring(1);
  }

  static String methodName(String prefix, String field) {
    return prefix + JavaCodeUtils.firstUpper(field);
  }

  static String getterName(String name, boolean isBoolean) {
    return isBoolean ? methodName("is", name) : methodName("get", name);
  }

  static String getterName(String name) {
    return getterName(name, false);
  }

  static String setterName(String name) {
    return methodName("set", name);
  }

  static String stripIndent(CharSequence code) {
    return StringUtils.stripIndent(code);
  }
}
