/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestJavaEnum {

  private JavaContext context;

  private JavaWriter writer;

  @BeforeEach
  public void beforeEach() {
    context = new JavaContext("com.axelor.some");
    writer = new JavaWriter(context, "  ");
  }

  @Test
  public void testSimple() {
    JavaType pojo = JavaType.newEnum("Hello", Modifier.PUBLIC);

    pojo.enumConstant("ONE");
    pojo.enumConstant("TWO");

    String code = writer.emit(pojo).toString();

    assertEquals("public enum Hello {ONE,TWO}", code.replaceAll("\n\\s*", ""));
  }

  @Test
  public void testConstructor() {
    JavaType pojo = JavaType.newEnum("Hello", Modifier.PUBLIC);

    pojo.enumConstant("ONE", 1);
    pojo.enumConstant("TWO", 2);

    pojo.field("value", "int", Modifier.PRIVATE | Modifier.FINAL);

    JavaMethod ctor = new JavaMethod("Hello", null);
    ctor.param("value", "int");
    ctor.code("this.value = value;");
    pojo.constructor(ctor);

    JavaMethod num = new JavaMethod("number", "int", Modifier.PUBLIC);
    num.code("return value;");

    pojo.method(num);

    String code = writer.emit(pojo).toString();
    String expected =
        "public enum Hello {ONE(1),TWO(2);private final int value;Hello(int value) {this.value = value;}public int number() {return value;}}";

    assertEquals(expected, code.replaceAll("\n\\s*", ""));
  }

  @Test
  public void testAnnotations() {
    JavaType pojo = JavaType.newEnum("Hello", Modifier.PUBLIC);
    JavaEnumConstant item = new JavaEnumConstant("ONE", 1);

    pojo.annotation(new JavaAnnotation("XmlType"));
    item.annotation(new JavaAnnotation("Title").param("value", "{0:s}", "One"));

    pojo.enumConstant(item);

    String code = writer.emit(pojo).toString();
    String expected = "@XmlTypepublic enum Hello {@Title(\"One\")ONE(1)}";

    assertEquals(expected, code.replaceAll("\n\\s*", ""));
  }
}
