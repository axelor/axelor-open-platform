/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import com.axelor.db.tenants.TenantResolver;
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
    return distributedService.getLock(scopedName(stackWalker.getCallerClass(), name));
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
    return distributedService.getLockIfDistributed(scopedName(stackWalker.getCallerClass(), name));
  }

  /**
   * Returns distributed-aware atomic long.
   *
   * @param name
   * @return distributed-aware atomic long
   */
  public static DistributedAtomicLong getAtomicLong(String name) {
    return distributedService.getAtomicLong(scopedName(stackWalker.getCallerClass(), name));
  }

  /**
   * Returns a distributed publish/subscribe topic.
   *
   * <p>The caller class is used together with the specified name as topic name.
   *
   * <p>This is not scoped by tenant. Instead, publishers send current tenant identifier which is
   * applied before calling listeners.
   *
   * @param name name of the topic
   * @return distributed topic, or a local in-process topic if the cache is not distributed
   */
  public static AxelorTopic getTopic(String name) {
    return distributedService.getTopic(stackWalker.getCallerClass().getName() + ":" + name);
  }

  /** Builds the backing key for the given caller and name, scoped to the current tenant. */
  private static String scopedName(Class<?> caller, String name) {
    final String base = caller.getName() + ":" + name;
    final String tenantId = TenantResolver.currentTenantIdentifier();
    return tenantId == null ? base : tenantId + ":" + base;
  }
}
