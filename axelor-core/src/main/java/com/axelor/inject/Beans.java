/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.inject;

import com.google.inject.Injector;
import com.google.inject.Key;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;

/**
 * A singleton class that can be used to get instances of injetable services where injection is not
 * possible.
 */
@Singleton
public final class Beans {

  private static Beans instance;

  private Injector injector;

  @Inject
  private Beans(Injector injector) {
    this.injector = injector;
    instance = this;
  }

  private static Beans get() {
    if (instance == null || instance.injector == null) {
      throw new RuntimeException("Guice is not initialized.");
    }
    return instance;
  }

  /**
   * Returns the appropriate instance for the given injection type.
   *
   * @param <T> type of the requested bean
   * @param type the requested type
   * @return an appropriate instance of the given type
   */
  public static <T> T get(Class<T> type) {
    return get().injector.getInstance(type);
  }

  /**
   * Returns the appropriate instance for the given injection type qualified by the given
   * annotation.
   *
   * @param <T> type of the requested bean
   * @param type the requested type
   * @param qualifier the qualifier annotation
   * @return an appropriate instance of the given type
   */
  public static <T> T get(Class<T> type, Annotation qualifier) {
    return get().injector.getInstance(Key.get(type, qualifier));
  }

  /**
   * Injects dependencies into the fields and methods of {@code bean}.
   *
   * @param <T> type of the bean
   * @param bean to inject members on
   * @return the bean itself
   */
  public static <T> T inject(T bean) {
    get().injector.injectMembers(bean);
    return bean;
  }
}
