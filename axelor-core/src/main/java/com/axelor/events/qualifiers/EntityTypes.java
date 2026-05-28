/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.events.qualifiers;

import java.lang.annotation.Annotation;
import java.util.Objects;

public final class EntityTypes {

  private EntityTypes() {}

  public static EntityType type(Class<?> type) {
    return new EntityTypeImpl(type);
  }

  @SuppressWarnings("all")
  private static final class EntityTypeImpl implements EntityType {

    private final Class<?> value;

    public EntityTypeImpl(Class<?> value) {
      this.value = value;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return EntityType.class;
    }

    @Override
    public Class<?> value() {
      return this.value;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof EntityType entityType && entityType.value().isAssignableFrom(value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(31, value);
    }
  }
}
