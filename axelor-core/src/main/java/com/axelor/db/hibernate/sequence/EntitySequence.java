/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.sequence;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.hibernate.annotations.IdGeneratorType;

/**
 * Annotation for configuring database sequence-based ID generation for JPA entities.
 *
 * @see EntitySequenceIdGenerator
 */
@IdGeneratorType(EntitySequenceIdGenerator.class)
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface EntitySequence {

  /** Name of the sequence */
  String name();

  /** Allocation size (>0 to override config) */
  int allocationSize() default -1;
}
