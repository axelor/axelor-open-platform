/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** This annotation can be used to specify custom change tracking messages. */
@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackMessage {

  /**
   * The track message to generate if the given condition is true.
   *
   * @return the message
   */
  String message();

  /**
   * The condition to check.
   *
   * @return the condition
   */
  String condition();

  /**
   * Specify the event on which to use this message.
   *
   * @return the event
   */
  TrackEvent on() default TrackEvent.DEFAULT;

  /**
   * Provide tag style if this message is a tag.
   *
   * <ul>
   *   <li>success
   *   <li>warning
   *   <li>important
   *   <li>info
   * </ul>
   *
   * @return the tag style name
   */
  String tag() default "";

  /**
   * Only use the message if these fields are changed.
   *
   * @return the field names
   */
  String[] fields() default "";
}
