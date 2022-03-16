/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class TestClassUtils {

  @Test
  public void testClassLoaderUtils() {
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    assertEquals(cl, ClassUtils.getContextClassLoader());
    assertEquals(cl, ClassUtils.getDefaultClassLoader());
    try {
      ClassUtils.setContextClassLoader(cl.getParent());
      assertNotEquals(cl, ClassUtils.getContextClassLoader());
    } finally {
      ClassUtils.setContextClassLoader(cl);
    }
  }

  @Test
  public void testNames() {
    String resourceName = "com/axelor/common/TestClassUtils.class";
    String className = "com.axelor.common.TestClassUtils";
    assertEquals(resourceName, ClassUtils.classToResourceName(className));
    assertEquals(className, ClassUtils.resourceToClassName(resourceName));
  }
}
