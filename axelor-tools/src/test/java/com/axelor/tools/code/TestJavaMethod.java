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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestJavaMethod {

  private JavaContext context;

  private JavaWriter writer;

  @BeforeEach
  public void beforeEach() {
    context = new JavaContext("com.axelor.some");
    writer = new JavaWriter(context, "  ");
  }

  @Test
  public void testBasic() {
    JavaMethod method = new JavaMethod("hello", "void", Modifier.PUBLIC);
    String code = writer.emit(method).toString();
    assertEquals("public void hello() {}", code.replaceAll("\n", ""));
  }

  @Test
  public void testParams() {
    JavaMethod method = new JavaMethod("create", "com.axelor.some.Message", Modifier.PUBLIC);
    method.param("message", "String");
    method.param(
        new JavaParam("file", "java.io.File", true).annotation(new JavaAnnotation("NotNull")));

    String code = writer.emit(method).toString();
    assertEquals(
        ""
            + "public Message create(\n"
            + "  String message,\n"
            + "  @NotNull\n"
            + "  final File file\n"
            + ") {\n"
            + "}\n",
        code);
  }

  @Test
  public void testThrows() {
    JavaMethod method = new JavaMethod("hello", "void", Modifier.PUBLIC);
    method.throwable("java.io.IOException");
    method.throwable("javax.persistence.PersistenceException");
    String code = writer.emit(method).toString();
    assertEquals(
        "public void hello() throws IOException, PersistenceException {}",
        code.replaceAll("\n", ""));
  }

  @Test
  public void testBody() {
    JavaMethod method = new JavaMethod("say", "void", Modifier.PUBLIC);
    method.param("message", "String");
    method.code("if (message != null) {");
    method.code("  System.out.println(message);");
    method.code("}");

    String[] lines = writer.emit(method).toString().split("\n");
    String[] expecteds = {
      "public void say(String message) {",
      "  if (message != null) {",
      "    System.out.println(message);",
      "  }",
      "}"
    };

    assertArrayEquals(expecteds, lines);
  }

  @Test
  public void testJavaDoc() {
    JavaMethod method = new JavaMethod("say", "void", Modifier.PUBLIC);
    method.doc(new JavaDoc("Say what you want..."));
    String code = writer.emit(method).toString();
    assertTrue(code.contains("/**"));
    assertTrue(code.contains(" * Say what you want..."));
    assertTrue(code.contains(" */"));
  }

  @Test
  public void testAnnotation() {
    JavaMethod method = new JavaMethod("say", "void", Modifier.PUBLIC);
    JavaAnnotation annotation = new JavaAnnotation("java.lang.Override");
    method.annotation(annotation);
    String code = writer.emit(method).toString();
    assertTrue(code.contains("@Override"));
  }
}
