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
