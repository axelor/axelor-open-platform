/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
