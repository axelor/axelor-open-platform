/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
