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
