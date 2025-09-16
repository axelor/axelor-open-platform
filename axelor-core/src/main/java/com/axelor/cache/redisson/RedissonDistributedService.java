/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.axelor.cache.DistributedAtomicLong;
import com.axelor.cache.DistributedService;
import java.util.concurrent.locks.Lock;

public class RedissonDistributedService implements DistributedService {

  protected static final String LOCK_PREFIX = "axelor-lock:";
  protected static final String ATOMIC_PREFIX = "axelor-atomic:";

  @Override
  public Lock getLock(String name) {
    return RedissonProvider.get().getLock(LOCK_PREFIX + name);
  }

  @Override
  public DistributedAtomicLong getAtomicLong(String name) {
    return new RedissonAtomicLongAdapter(
        RedissonProvider.get().getAtomicLong(ATOMIC_PREFIX + name));
  }
}
