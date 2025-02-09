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
        .put(String.format("application.openapi.scan.%s", key), value);
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
