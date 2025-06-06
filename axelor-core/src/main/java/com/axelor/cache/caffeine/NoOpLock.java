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
