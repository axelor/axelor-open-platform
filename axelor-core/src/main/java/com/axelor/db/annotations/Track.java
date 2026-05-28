/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.annotations;

import com.axelor.db.Model;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** This annotation can be used on {@link Model} classes to provide change track details. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Track {

  /**
   * The fields to track.
   *
   * @return the fields to track
   */
  TrackField[] fields() default {};

  /**
   * The track messages to generate.
   *
   * @return the messages
   */
  TrackMessage[] messages() default {};

  /**
   * The body content to generate.
   *
   * @return the body messages
   */
  TrackMessage[] contents() default {};

  /**
   * Subscribe for change notifications.
   *
   * @return true if marked for auto-subscribe
   */
  boolean subscribe() default false;

  /**
   * Whether to track attached files.
   *
   * @return true if marked for tracking attachments
   */
  boolean files() default false;

  /**
   * Specify the event on which to track.
   *
   * @return the event
   */
  TrackEvent on() default TrackEvent.ALWAYS;
}
