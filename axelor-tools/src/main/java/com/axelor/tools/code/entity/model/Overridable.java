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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BiConsumer;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Overridable {

  /**
   * Returns class for checking whether the attribute is overridable.
   *
   * @return override checker
   */
  Class<? extends BiConsumer<Object, Object>> value() default DefaultOverridable.class;
}

class DefaultOverridable implements BiConsumer<Object, Object> {
  @Override
  public void accept(Object t, Object u) {
    // Always allow
  }
}
