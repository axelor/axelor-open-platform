/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** This annotation can be used to specify details about field for change tracking. */
@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackField {

  /**
   * The field name to track.
   *
   * @return the field name
   */
  String name();

  /**
   * The condition to check.
   *
   * @return the condition
   */
  String condition() default "";

  /**
   * Specify the event on which to track.
   *
   * @return the event
   */
  TrackEvent on() default TrackEvent.DEFAULT;
}
