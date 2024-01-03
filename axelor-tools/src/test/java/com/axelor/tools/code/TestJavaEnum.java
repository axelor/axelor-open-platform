/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
