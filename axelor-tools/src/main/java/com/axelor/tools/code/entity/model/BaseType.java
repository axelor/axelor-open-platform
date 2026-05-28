/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code.entity.model;

import com.axelor.tools.code.JavaType;

public interface BaseType<T> {

  String getName();

  String getPackageName();

  JavaType toJavaClass();

  default JavaType toRepoClass() {
    return null;
  }

  void merge(T other);
}
