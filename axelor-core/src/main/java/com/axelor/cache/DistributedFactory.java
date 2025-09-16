/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import java.util.concurrent.locks.Lock;

public class DistributedFactory {

  private static final StackWalker stackWalker =
      StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

  private DistributedFactory() {}

  static DistributedService distributedService =
      CacheBuilder.getCacheType().getDistributedService();

  /**
   * Returns general-purpose distributed-aware lock.
   *
   * @param name name of the lock
   * @return distributed-aware reentrant lock
   */
  public static Lock getLock(String name) {
    return distributedService.getLock(stackWalker.getCallerClass().getName() + ":" + name);
  }

  /**
   * Returns a lock that locks only when the cache is distributed.
   *
   * <p>This is useful when no locking is needed in single-instance setup.
   *
   * @param name name of the lock
   * @return distributed reentrant lock or no-op lock if cache is not distributed
   */
  public static Lock getLockIfDistributed(String name) {
    return distributedService.getLockIfDistributed(name);
  }

  /**
   * Returns distributed-aware atomic long.
   *
   * @param name
   * @return distributed-aware atomic long
   */
  public static DistributedAtomicLong getAtomicLong(String name) {
    return distributedService.getAtomicLong(stackWalker.getCallerClass().getName() + ":" + name);
  }
}
