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
package com.axelor.db.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Provides information about UI widget representing this field. */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Widget {

  /**
   * Title for the UI widget.
   *
   * @return the title
   */
  String title() default "";

  /**
   * Help text.
   *
   * @return help text
   */
  String help() default "";

  /**
   * Whether the widget should be set readonly.
   *
   * @return true if marked as readonly, default is false
   */
  boolean readonly() default false;

  /**
   * Whether to hide widget by default.
   *
   * @return true if marked as hidden, default is false
   */
  boolean hidden() default false;

  /**
   * Should be used with String fields to mark whether to use multiline text widget.
   *
   * @return true if marked as multiline, default is false
   */
  boolean multiline() default false;

  /**
   * Use image widget for this binary field.
   *
   * @return true if marked as image, default is false
   */
  boolean image() default false;

  /**
   * Use password widget for this string field.
   *
   * @return true if marked as password, default is false
   */
  boolean password() default false;

  /**
   * Whether to allow mass update on this field.
   *
   * @return true if marked for mass update, default is false
   */
  boolean massUpdate() default false;

  /**
   * Whether to copy this field when creating duplicate record.
   *
   * @return true if marked as copyable, default is true
   */
  boolean copyable() default true;

  /**
   * Whether the field value is translatable.
   *
   * @return true if marked as translatable, default is false
   */
  boolean translatable() default false;

  /**
   * Whether the field should use current date/datetime as default value.
   *
   * @return true if "now" is given as defaultValue
   */
  boolean defaultNow() default false;

  /**
   * List of the columns to be used to search this record.
   *
   * <p>Used by auto-complete widget. By default the same column will be searched. Also, in case of
   * virtual column (computed values) specify the actual searchable columns.
   *
   * @return array if fields on which to search, default is empty
   */
  String[] search() default {};

  /**
   * The name of the selection.
   *
   * @return name of the selection, default is empty
   */
  String selection() default "";
}
