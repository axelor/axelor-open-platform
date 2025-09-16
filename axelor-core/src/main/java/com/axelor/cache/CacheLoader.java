/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import jakarta.annotation.Nullable;

/**
 * Computes or retrieves values, based on a key, for use in populating an {@link AxelorCache}.
 *
 * <p>Warning: loading must not attempt to update any mappings of the cache directly.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
@FunctionalInterface
public interface CacheLoader<K, V> {

  /**
   * Computes or retrieves the value corresponding to {@code key}.
   *
   * @param key the non-null key whose value should be loaded
   * @return the value associated with {@code key} or {@code null} if not found
   */
  @Nullable
  V load(K key);
}
