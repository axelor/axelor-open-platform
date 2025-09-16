/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

/**
 * Common atomic long operations found in both
 *
 * <p>{@link java.util.concurrent.atomic.AtomicLong} and {@link org.redisson.api.RAtomicLong}.
 */
public interface DistributedAtomicLong {

  /**
   * Returns the current value.
   *
   * @return the current value
   */
  long get();

  /**
   * Sets the value to {@code newValue}.
   *
   * @param newValue the new value
   */
  void set(long newValue);

  /**
   * Atomically sets the value to {@code newValue} and returns the old value.
   *
   * @param newValue the new value
   * @return the previous value
   */
  long getAndSet(long newValue);

  /**
   * Atomically sets the value to {@code newValue} if the current value {@code == expectedValue}.
   *
   * @param expectedValue the expected value
   * @param newValue the new value
   * @return {@code true} if successful. False return indicates that the actual value was not equal
   *     to the expected value.
   */
  boolean compareAndSet(long expectedValue, long newValue);

  /**
   * Atomically increments the current value.
   *
   * <p>Equivalent to {@code addAndGet(1)}.
   *
   * @return the updated value
   */
  long getAndIncrement();

  /**
   * Atomically decrements the current value.
   *
   * @return the previous value
   */
  long getAndDecrement();

  /**
   * Atomically increments the current value.
   *
   * <p>Equivalent to {@code addAndGet(1)}.
   *
   * @return the updated value
   */
  long getAndAdd(long delta);

  /**
   * Atomically increments the current value by one.
   *
   * @return the updated value (value after incrementing)
   */
  long incrementAndGet();

  /**
   * Atomically decrements the current value.
   *
   * <p>Equivalent to {@code addAndGet(-1)}.
   *
   * @return the updated value
   */
  long decrementAndGet();

  /**
   * Atomically adds the given value to the current value.
   *
   * @param delta the value to add
   * @return the updated value
   */
  long addAndGet(long delta);

  /** Returns underlying atomic long. */
  Object getUnderlyingAtomicLong();
}
