/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import java.time.Duration;

/** Cache that supports expiration on the cache itself */
public interface ExpirableAxelorCache<K, V> extends AxelorCache<K, V> {
  /**
   * Sets a time to live.
   *
   * @param ttl time to live
   * @return <code>true</code> if the time to live was set
   */
  boolean expire(Duration ttl);

  /**
   * Clears the time to live.
   *
   * @return <code>true</code> if the time to live was removed
   */
  boolean clearExpire();

  /**
   * Returns the remaining time to live in milliseconds.
   *
   * @return remaining time to live in milliseconds, or a negative value if there is no time to live
   */
  long remainTimeToLive();
}
