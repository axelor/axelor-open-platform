/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.event;

/** The reason why a cached entry was removed */
public enum RemovalCause {
  /** Removed because of invalidation or eviction */
  REMOVED,

  /** Replaced with another value */
  REPLACED,

  /** Removed because of expiration */
  EXPIRED,
}
