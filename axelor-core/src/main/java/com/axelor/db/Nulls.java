/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

/**
 * Specifies the precedence of null values within query result sets.
 *
 * <p>This is a backport of jakarta.persistence.criteria.Nulls from Jakarta Persistence 3.2, in
 * order to avoid using implementation-specific {@link org.hibernate.query.NullPrecedence}.
 */
public enum Nulls {
  /** Null precedence not specified. */
  NONE,
  /** Null values occur at the beginning of the result set. */
  FIRST,
  /** Null values occur at the end of the result set. */
  LAST
}
