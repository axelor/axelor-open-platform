/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.mapper.types;

import com.axelor.common.StringUtils;
import com.axelor.common.UuidUtils;
import com.axelor.db.mapper.TypeAdapter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.UUID;

public class UUIDAdapter implements TypeAdapter<UUID> {

  @Override
  public Object adapt(
      Object value, Class<?> actualType, Type genericType, Annotation[] annotations) {

    // Handle null or UUID directly
    if (value == null || value instanceof UUID) {
      return value;
    }

    // Convert CharSequence to UUID
    if (value instanceof CharSequence s) {
      return StringUtils.isBlank(s) ? null : UuidUtils.parse(s.toString());
    }

    return value;
  }
}
