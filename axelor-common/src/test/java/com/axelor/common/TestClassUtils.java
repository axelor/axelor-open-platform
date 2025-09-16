/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
