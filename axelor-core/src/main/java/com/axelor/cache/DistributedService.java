/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
