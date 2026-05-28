/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import java.util.concurrent.locks.Lock;

/** Service for getting distributed-aware objects. */
public interface DistributedService {

  /**
   * Returns general-purpose distributed-aware lock.
   *
   * @param name name of the lock
   * @return distributed-aware reentrant lock
   */
  Lock getLock(String name);

  /**
   * Returns a lock that locks only when the cache is distributed.
   *
   * <p>This is useful when no locking is needed in single-instance setup.
   *
   * @param name name of the lock
   * @return distributed reentrant lock or no-op lock if cache is not distributed
   */
  default Lock getLockIfDistributed(String name) {
    return getLock(name);
  }

  /**
   * Returns distributed-aware atomic long.
   *
   * @param name
   * @return distributed-aware atomic long
   */
  DistributedAtomicLong getAtomicLong(String name);
}
