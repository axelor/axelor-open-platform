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
