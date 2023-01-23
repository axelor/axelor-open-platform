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
