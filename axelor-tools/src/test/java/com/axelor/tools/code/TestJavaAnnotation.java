/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestJavaAnnotation {

  private JavaContext context;

  private JavaWriter writer;

  @BeforeEach
  public void beforeEach() {
    context = new JavaContext("com.axelor.some");
    writer = new JavaWriter(context, "  ");
  }

  @Test
  public void testValue() {
    JavaAnnotation annotation = new JavaAnnotation("com.axelor.some.SomeTest");
    annotation.param("value", "{0:m}", "com.axelor.another.Test.class");
    writer.emit(annotation);
    String code = writer.toString();
    assertEquals("@SomeTest(Test.class)", code);
  }

  @Test
  public void testParam() {
    JavaAnnotation annotation = new JavaAnnotation("com.axelor.some.SomeTest");
    annotation.param("type", "{0:m}", "com.axelor.another.Test.class");
    writer.emit(annotation);
    String code = writer.toString();
    assertEquals("@SomeTest(type=Test.class)", code.replaceAll("\\s", ""));
  }

  @Test
  public void testArrayValue() {
    JavaAnnotation annotation = new JavaAnnotation("com.axelor.some.SomeTest");
    annotation.param("value", "{0:m}", "com.axelor.another.Test.class");
    annotation.param("value", "{0:m}", "com.axelor.another.AnotherTest.class");
    writer.emit(annotation);
    String code = writer.toString();
    assertEquals("@SomeTest({Test.class,AnotherTest.class})", code.replaceAll("\\s", ""));
  }

  @Test
  public void testArrayParam() {
    JavaAnnotation annotation = new JavaAnnotation("com.axelor.some.SomeTest");
    annotation.param("type", "{0:m}", "com.axelor.another.Test.class");
    annotation.param("type", "{0:m}", "com.axelor.another.AnotherTest.class");
    writer.emit(annotation);
    String code = writer.toString();
    assertEquals("@SomeTest(type={Test.class,AnotherTest.class})", code.replaceAll("\\s", ""));
  }

  @Test
  public void testMixValueParam() {
    JavaAnnotation annotation = new JavaAnnotation("com.axelor.some.SomeTest");
    annotation.param("value", "{0:m}", "com.axelor.another.Test.class");
    annotation.param("type", "{0:m}", "com.axelor.another.AnotherTest.class");
    writer.emit(annotation);
    String code = writer.toString();
    assertEquals("@SomeTest(value=Test.class,type=AnotherTest.class)", code.replaceAll("\\s", ""));
  }

  @Test
  public void testAnnotationParam() {
    JavaAnnotation annotation = new JavaAnnotation("com.axelor.some.SomeTest");
    JavaAnnotation param = new JavaAnnotation("com.axelor.some.Another");

    param.param("value", "{0:m}", "com.axelor.another.Test.class");
    param.param("value", "{0:m}", "com.axelor.another.AnotherTest.class");
    annotation.param("value", param);

    writer.emit(annotation);
    String code = writer.toString();

    assertEquals("@SomeTest(@Another({Test.class,AnotherTest.class}))", code.replaceAll("\\s", ""));
  }
}
