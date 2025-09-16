/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.app.AppSettings;
import com.axelor.web.openapi.AxelorOpenApiScanner;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OpenAPIScannerTest {

  @Test
  public void simple() throws Exception {
    // default ignored
    assertTrue(isClassIgnored("org.jboss.resteasy.core.AsynchronousDispatcher"));
    assertFalse(isClassIgnored("com.axelor.pck1.MyClass"));

    addScanProp("exclude.classes", "com.axelor.pck1.sub1.MyClass1,com.axelor.pck1.MyOther1");
    assertTrue(isClassIgnored("com.axelor.pck1.sub1.MyClass1"));
    assertTrue(isClassIgnored("com.axelor.pck1.MyOther1"));
    assertFalse(isClassIgnored("com.axelor.pck1.sub1.MyClass2"));
    assertFalse(isClassIgnored("com.axelor.pck1.sub1.sub2.MyClass2"));
    assertFalse(isClassIgnored("com.axelor.pck1.MyClass"));

    addScanProp("exclude.packages", "com.axelor.pck1.sub1");
    assertTrue(isClassIgnored("com.axelor.pck1.sub1.MyClass1"));
    assertTrue(isClassIgnored("com.axelor.pck1.sub1.MyClass3"));
    assertTrue(isClassIgnored("com.axelor.pck1.sub1.sub2.MyClass3"));
    assertFalse(isClassIgnored("com.axelor.pck1.MyClass"));

    addScanProp("classes", "com.axelor.pck1.sub1.MyClass, com.axelor.pck1.sub1.MyClass1");
    assertFalse(isClassIgnored("com.axelor.pck1.sub1.MyClass"));
    assertTrue(isClassIgnored("com.axelor.pck1.sub1.MyClass1"));
    assertTrue(isClassIgnored("com.axelor.pck1.sub1.MyClass3"));

    addScanProp("packages", "com.axelor.pck1.sub1,com.axelor.pck1.sub1.sub2");
    assertTrue(isClassIgnored("com.axelor.pck1.sub1.MyClass1"));
    assertFalse(isClassIgnored("com.axelor.pck1.sub1.sub2.MyClass5"));
  }

  @Test
  public void excludeClassesIfIncludeIsSet() throws Exception {
    assertFalse(isClassIgnored("com.axelor.pck2.MyClass"));
    assertFalse(isClassIgnored("com.axelor.pck2.sub1.MyClass"));

    addScanProp("classes", "com.axelor.pck1.MyClass");
    assertFalse(isClassIgnored("com.axelor.pck1.MyClass"));
    assertTrue(isClassIgnored("com.axelor.pck2.MyClass"));

    addScanProp("packages", "com.axelor.pck2.sub1");
    assertFalse(isClassIgnored("com.axelor.pck2.sub1.MyClass"));
    assertTrue(isClassIgnored("com.axelor.pck2.sub2.MyClass"));
  }

  private boolean isClassIgnored(String className) throws Exception {
    AxelorOpenApiScanner myClass = new AxelorOpenApiScanner();
    Method method = AxelorOpenApiScanner.class.getDeclaredMethod("isIgnored", String.class);
    method.setAccessible(true);
    return (boolean) method.invoke(myClass, className);
  }

  private void addScanProp(String key, String value) {
    AppSettings.get()
        .getInternalProperties()
        .put("application.openapi.scan.%s".formatted(key), value);
  }

  @BeforeEach
  void setup() {
    // Reset settings
    System.getProperties().stringPropertyNames().stream()
        .filter(it -> it.startsWith("axelor.config"))
        .forEach(System::clearProperty);

    try {
      Field instance = AppSettings.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);
    } catch (Exception e) {
      throw new RuntimeException();
    }
  }
}
