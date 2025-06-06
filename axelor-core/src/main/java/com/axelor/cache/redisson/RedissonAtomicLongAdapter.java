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
package com.axelor.cache.redisson;

import com.axelor.cache.DistributedAtomicLong;
import org.redisson.api.RAtomicLong;

/**
 * Adapter class for {@link org.redisson.api.RAtomicLong} to conform to the {@link
 * DistributedAtomicLong} interface.
 */
public class RedissonAtomicLongAdapter implements DistributedAtomicLong {

  private final RAtomicLong atomicLong;

  public RedissonAtomicLongAdapter(RAtomicLong atomicLong) {
    this.atomicLong = atomicLong;
  }

  @Override
  public long get() {
    return atomicLong.get();
  }

  @Override
  public void set(long newValue) {
    atomicLong.set(newValue);
  }

  @Override
  public long getAndSet(long newValue) {
    return atomicLong.getAndSet(newValue);
  }

  @Override
  public boolean compareAndSet(long expectedValue, long newValue) {
    return atomicLong.compareAndSet(expectedValue, newValue);
  }

  @Override
  public long getAndIncrement() {
    return atomicLong.getAndIncrement();
  }

  @Override
  public long getAndDecrement() {
    return atomicLong.getAndDecrement();
  }

  @Override
  public long getAndAdd(long delta) {
    return atomicLong.getAndAdd(delta);
  }

  @Override
  public long incrementAndGet() {
    return atomicLong.incrementAndGet();
  }

  @Override
  public long decrementAndGet() {
    return atomicLong.decrementAndGet();
  }

  @Override
  public long addAndGet(long delta) {
    return atomicLong.addAndGet(delta);
  }

  @Override
  public RAtomicLong getUnderlyingAtomicLong() {
    return atomicLong;
  }
}
