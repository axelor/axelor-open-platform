/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
