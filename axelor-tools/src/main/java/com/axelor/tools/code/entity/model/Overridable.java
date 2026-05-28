/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
