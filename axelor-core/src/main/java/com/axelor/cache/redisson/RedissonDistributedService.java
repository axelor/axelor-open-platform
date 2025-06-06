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
