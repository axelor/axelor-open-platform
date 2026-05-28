/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
