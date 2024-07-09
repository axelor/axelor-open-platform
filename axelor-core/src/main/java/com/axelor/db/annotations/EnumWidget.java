/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.db.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Provides information about UI widget representing this enumeration constant. */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumWidget {

  /**
   * Title for the UI.
   *
   * @return the title
   */
  String title() default "";

  /**
   * Description text.
   *
   * @return description text
   */
  String description() default "";

  /**
   * Image icon to show for this item.
   *
   * @return icon name
   */
  String icon() default "";

  /**
   * Whether to hide enum constant.
   *
   * @return true if marked as hidden, default is false
   */
  boolean hidden() default false;

  /**
   * Sequence number to order the option.
   *
   * @return the sequence number
   */
  int order() default 0;
}
