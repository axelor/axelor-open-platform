/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.mapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public interface TypeAdapter<T> {

  Object adapt(Object value, Class<?> actualType, Type genericType, Annotation[] annotations);
}
