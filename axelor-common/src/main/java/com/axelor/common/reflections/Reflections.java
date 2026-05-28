/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.reflections;

/**
 * The {@link Reflections} utilities provides fast and easy way to search for resources and types.
 */
public final class Reflections {

  private Reflections() {}

  /**
   * Return a {@link ClassFinder} to search for the sub types of the given type.
   *
   * @param <T> the type to search
   * @param type the super type
   * @param loader find with the given {@link ClassLoader}
   * @return an instance of {@link ClassFinder}
   */
  public static <T> ClassFinder<T> findSubTypesOf(Class<T> type, ClassLoader loader) {
    return new ClassFinder<>(type, loader);
  }

  /**
   * Return a {@link ClassFinder} to search for the sub types of the given type.
   *
   * @param <T> the type to search
   * @param type the super type
   * @return an instance of {@link ClassFinder}
   */
  public static <T> ClassFinder<T> findSubTypesOf(Class<T> type) {
    return new ClassFinder<>(type);
  }

  /**
   * Return a {@link ClassFinder} to search for types.
   *
   * @param loader find with the given {@link ClassLoader}
   * @return an instance of {@link ClassFinder}
   */
  public static ClassFinder<?> findTypes(ClassLoader loader) {
    return findSubTypesOf(Object.class, loader);
  }

  /**
   * Return a {@link ClassFinder} to search for types.
   *
   * @return an instance of {@link ClassFinder}
   */
  public static ClassFinder<?> findTypes() {
    return findSubTypesOf(Object.class);
  }

  /**
   * Return a {@link ResourceFinder} to search for resources.
   *
   * @param loader find with the given {@link ClassLoader}
   * @return an instance of {@link ResourceFinder}
   */
  public static ResourceFinder findResources(ClassLoader loader) {
    return new ResourceFinder(loader);
  }

  /**
   * Return a {@link ResourceFinder} to search for resources.
   *
   * @return an instance of {@link ResourceFinder}
   */
  public static ResourceFinder findResources() {
    return new ResourceFinder();
  }
}
