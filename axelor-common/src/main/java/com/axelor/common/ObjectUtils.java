/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** This class defines from static helper methods to deal with objects. */
public final class ObjectUtils {

  /**
   * Check whether the given value is an array.
   *
   * @param value the value to check
   * @return true if value is array false otherwise
   */
  public static boolean isArray(Object value) {
    return value != null && value.getClass().isArray();
  }

  /**
   * Check whether the given value is empty.
   *
   * <p>An object value is empty if:
   *
   * <ul>
   *   <li>value is null
   *   <li>value is {@link Optional} and {@link Optional#empty()}
   *   <li>value is {@link Array} with length 0
   *   <li>value is {@link CharSequence} with length 0
   *   <li>value is {@link Collection} or {@link Map} with size 0
   * </ul>
   *
   * @param value the object value to check
   * @return true if empty false otherwise
   */
  public static boolean isEmpty(Object value) {
    if (value == null) {
      return true;
    }
    if (value instanceof Optional<?> optional) {
      return optional.isEmpty();
    }
    if (value.getClass().isArray()) {
      return Array.getLength(value) == 0;
    }
    if (value instanceof CharSequence sequence) {
      return sequence.length() == 0;
    }
    if (value instanceof Collection<?> collection) {
      return collection.isEmpty();
    }
    if (value instanceof Map<?, ?> map) {
      return map.isEmpty();
    }
    return false;
  }

  /**
   * Check whether the given value is not empty.
   *
   * @param value the object value to check
   * @return true if not empty false otherwise
   * @see #isEmpty(Object)
   */
  public static boolean notEmpty(Object value) {
    return !isEmpty(value);
  }

  /**
   * Check whether the given map is mutable.
   *
   * @param map the map to check
   * @return true if mutable false otherwise
   */
  public static boolean isMutable(Map<?, ?> map) {
    if (isKnownImmutable(map)) {
      return false;
    }

    try {
      map.remove(new Object());
      return true;
    } catch (UnsupportedOperationException e) {
      return false;
    }
  }

  /**
   * Check whether the given collection is mutable.
   *
   * @param collection the collection to check
   * @return true if mutable false otherwise
   */
  public static boolean isMutable(Collection<?> collection) {
    if (isKnownImmutable(collection)) {
      return false;
    }

    try {
      collection.remove(new Object());
      return true;
    } catch (UnsupportedOperationException e) {
      return false;
    }
  }

  private static final List<String> KNOWN_IMMUTABLE_CLASS_PARTS =
      List.of("Immutable", "Unmodifiable", "$Empty", "$Singleton");

  private static boolean isKnownImmutable(Object object) {
    if (object == null) {
      return true;
    }

    var className = object.getClass().getName();
    return KNOWN_IMMUTABLE_CLASS_PARTS.stream().anyMatch(className::contains);
  }
}
