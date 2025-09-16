/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.caffeine;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/** A no-op lock. */
class NoOpLock implements Lock {

  @Override
  public void lock() {
    // no-op
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    // no-op
  }

  @Override
  public boolean tryLock() {
    return true;
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    return true;
  }

  @Override
  public void unlock() {
    // no-op
  }

  @Override
  public Condition newCondition() {
    return new AbstractQueuedSynchronizer() {}.new ConditionObject();
  }
}
