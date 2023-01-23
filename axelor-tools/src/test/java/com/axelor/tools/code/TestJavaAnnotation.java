/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
