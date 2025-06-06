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
package com.axelor.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DistributedFactoryTest {

  @ParameterizedTest(name = "{0} - Lock")
  @EnumSource(CacheType.class)
  void testGetLock(CacheType cacheType) {
    setDistributedService(cacheType);

    var lock = DistributedFactory.getLock("testLock");

    assertNotNull(lock);

    // Verify basic lock functionality
    lock.lock();
    try {
      assertTrue(true, "Lock should be acquired successfully");
    } finally {
      lock.unlock();
    }
  }

  @ParameterizedTest(name = "{0} - Atomic Long")
  @EnumSource(CacheType.class)
  void testGetAtomicLong(CacheType cacheType) {
    setDistributedService(cacheType);

    var atomicLong = DistributedFactory.getAtomicLong("testAtomicLong");

    assertNotNull(atomicLong);

    // Verify basic atomic long functionality
    long initialValue = atomicLong.get();
    atomicLong.set(10L);
    assertEquals(10L, atomicLong.get());
    atomicLong.set(initialValue);
  }

  @ParameterizedTest(name = "{0} - Namespacing")
  @EnumSource(CacheType.class)
  void testNamespacing(CacheType cacheType) {
    setDistributedService(cacheType);

    class AnotherCaller {
      DistributedAtomicLong getAtomicLong(String name) {
        return DistributedFactory.getAtomicLong(name);
      }
    }

    var longThisClass = DistributedFactory.getAtomicLong("sharedName");
    var anotherLongFromThisClass = DistributedFactory.getAtomicLong("sharedName");
    var longFromAnotherClass = new AnotherCaller().getAtomicLong("sharedName");

    assertNotNull(longThisClass);
    assertNotNull(anotherLongFromThisClass);
    assertNotNull(longFromAnotherClass);

    longThisClass.set(1L);
    longFromAnotherClass.set(10L);

    assertEquals(2L, longThisClass.incrementAndGet());
    assertEquals(2L, anotherLongFromThisClass.get());
    assertEquals(10L, longFromAnotherClass.get());
  }

  void setDistributedService(CacheType cacheType) {
    DistributedFactory.distributedService = cacheType.getDistributedService();
  }

  @BeforeAll
  static void setUp() {
    RedisTest.startRedis();
  }

  @AfterAll
  static void tearDown() {
    DistributedFactory.distributedService = CacheBuilder.getCacheType().getDistributedService();
    RedisTest.stopRedis();
  }
}
