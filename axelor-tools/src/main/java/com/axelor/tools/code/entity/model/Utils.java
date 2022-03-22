/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.tools.code.entity.model;

import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.tools.code.JavaCodeUtils;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

interface Utils {

  static boolean isBlank(String value) {
    return StringUtils.isBlank(value);
  }

  static boolean notBlank(String value) {
    return StringUtils.notBlank(value);
  }

  static boolean isEmpty(Object value) {
    return ObjectUtils.isEmpty(value);
  }

  static boolean notEmpty(Object value) {
    return !isEmpty(value);
  }

  static boolean isTrue(Boolean value) {
    return Boolean.TRUE.equals(value);
  }

  static boolean isFalse(Boolean value) {
    return Boolean.FALSE.equals(value);
  }

  static boolean notTrue(Boolean value) {
    return !isTrue(value);
  }

  static boolean notFalse(Boolean value) {
    return !isFalse(value);
  }

  static Stream<String> stream(String list) {
    return isBlank(list) ? Stream.empty() : Stream.of(list.split(",")).map(String::trim);
  }

  static List<String> list(String list) {
    return stream(list).collect(Collectors.toList());
  }

  static String methodName(String prefix, String field) {
    return JavaCodeUtils.methodName(prefix, field);
  }

  static String getterName(String name) {
    return JavaCodeUtils.getterName(name);
  }

  static String setterName(String name) {
    return JavaCodeUtils.setterName(name);
  }
}
