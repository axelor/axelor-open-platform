/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestJavaInterface {

  private JavaContext context;

  private JavaWriter writer;

  @BeforeEach
  public void beforeEach() {
    context = new JavaContext("com.axelor.some");
    writer = new JavaWriter(context, "  ");
  }

  @Test
  public void testSimple() {
    JavaType pojo =
        JavaType.newInterface("Hello", Modifier.PUBLIC).superInterface("com.example.World");

    String code = writer.emit(pojo).toString();

    assertEquals("public interface Hello extends World {}", code.replaceAll("\n\\s*", ""));
  }

  @Test
  public void testMethods() {
    JavaType pojo = JavaType.newInterface("Hello", Modifier.PUBLIC);

    JavaMethod say = new JavaMethod("say", "void").declaration(true);
    JavaMethod hello =
        new JavaMethod("hello", "void")
            .defaultMethod(true)
            .code("System.out.println({0:s});", "Hello!");

    pojo.method(say);
    pojo.method(hello);

    String code = writer.emit(pojo).toString();

    assertEquals(
        "public interface Hello {void say();default void hello() {System.out.println(\"Hello!\");}}",
        code.replaceAll("\n\\s*", ""));
  }
}
