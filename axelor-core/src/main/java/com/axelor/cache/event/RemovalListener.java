/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.event;

import jakarta.annotation.Nullable;

/**
 * An object that can receive a notification when an entry is removed from a cache.
 *
 * @param <K> the type of cache keys
 * @param <V> the type of cache values
 */
@FunctionalInterface
public interface RemovalListener<K, V> {

  /**
   * Notifies the listener that a removal occurred at some point in the past.
   *
   * @param key the key represented by this entry, or {@code null} if collected
   * @param value the value represented by this entry, or {@code null} if collected
   * @param cause the reason for which the entry was removed
   */
  void onRemoval(@Nullable K key, @Nullable V value, RemovalCause cause);
}
