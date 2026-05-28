/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.caffeine;

import com.axelor.cache.DistributedAtomicLong;
import com.axelor.cache.DistributedService;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CaffeineDistributedService implements DistributedService {

  private static LoadingCache<String, Lock> locks =
      Caffeine.newBuilder().weakValues().build(k -> new ReentrantLock());

  private static LoadingCache<String, DistributedAtomicLong> atomics =
      Caffeine.newBuilder().weakValues().build(k -> new AtomicLongAdapter(new AtomicLong()));

  private static final NoOpLock NO_OP_LOCK = new NoOpLock();

  @Override
  public Lock getLock(String name) {
    return locks.get(name);
  }

  @Override
  public Lock getLockIfDistributed(String name) {
    return NO_OP_LOCK;
  }

  @Override
  public DistributedAtomicLong getAtomicLong(String name) {
    return atomics.get(name);
  }
}
