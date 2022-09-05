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

import com.axelor.common.StringUtils;
import com.google.common.base.Preconditions;
import java.beans.PropertyDescriptor;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class PropertyAttribute {
  private final String name;
  private final PropertyDescriptor descriptor;

  private final BiConsumer<Object, Object> checkOverride;
  private final Predicate<Object> isAbsent;

  public static PropertyAttribute of(
      String name, PropertyDescriptor descriptor, BiConsumer<Object, Object> checkOverride) {
    return new PropertyAttribute(name, descriptor, checkOverride, Objects::isNull);
  }

  public static PropertyAttribute ofValue(
      String name, PropertyDescriptor descriptor, BiConsumer<Object, Object> checkOverride) {
    return new PropertyAttribute(
        name, descriptor, checkOverride, value -> StringUtils.isBlank(String.valueOf(value)));
  }

  private PropertyAttribute(
      String name,
      PropertyDescriptor descriptor,
      BiConsumer<Object, Object> checkOverride,
      Predicate<Object> isAbsent) {
    Preconditions.checkNotNull(descriptor);
    this.name = StringUtils.notBlank(name) ? name : descriptor.getName();
    this.descriptor = descriptor;
    this.checkOverride = checkOverride != null ? checkOverride : (a, b) -> {};
    this.isAbsent = isAbsent;
  }

  public String getName() {
    return name;
  }

  public PropertyDescriptor getDescriptor() {
    return descriptor;
  }

  public void checkOverride(Object a, Object b) {
    checkOverride.accept(a, b);
  }

  public boolean isAbsent(Object a) {
    return isAbsent.test(a);
  }
}
