/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

/** The class loader for domain objects. */
public class JpaClassLoader extends ClassLoader {

  public JpaClassLoader() {
    this(Thread.currentThread().getContextClassLoader());
  }

  public JpaClassLoader(ClassLoader parent) {
    super(parent);
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    try {
      return super.findClass(name);
    } catch (ClassNotFoundException e) {
      final Class<?> model = findModelClass(name);
      if (model != null) {
        return model;
      }
      throw e;
    }
  }

  /** try to find domain or repository class in cache. */
  private Class<?> findModelClass(String className) {
    if (!className.startsWith("java.lang.") || className.contains("$")) {
      return null;
    }
    String name = className.substring(className.lastIndexOf('.') + 1);
    Class<?> klass = JpaScanner.findRepository(name);
    if (klass == null) {
      klass = JpaScanner.findModel(name);
    }
    if (klass == null) {
      klass = JpaScanner.findEnum(name);
    }
    return klass;
  }
}
