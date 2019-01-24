/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
      return obj instanceof EntityType && ((EntityType) obj).value().isAssignableFrom(value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(31, value);
    }
  }
}
