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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestJavaContext {

  private JavaContext context;

  @BeforeEach
  public void beforeEach() {
    context = new JavaContext("com.demo.example");
  }

  @Test
  public void testImportSimple() {
    String name;

    // don't generate import statement for current package
    name = context.importType("com.demo.example.SomeClass");
    assertEquals("SomeClass", name);

    // don't generate import statement for simple name
    name = context.importType("SomeClass");
    assertEquals("SomeClass", name);

    // generate import statement for any other packages
    name = context.importType("com.demo.some.example.Hello");
    assertEquals("Hello", name);
  }

  @Test
  public void testImportGeneric() {
    // import generic types
    String name = context.importType("java.util.Map<String, com.some.example.Person>");
    assertEquals("Map<String, Person>", name);
  }

  @Test
  public void testImportJavaLang() {
    // don't generate import statement for "java.lang"
    String name = context.importType("java.lang.String");
    assertEquals("String", name);
  }

  @Test
  public void testImportStatic() {
    // import static
    String name = context.importStatic("org.junit.jupiter.api.Assertions.assertEquals");
    assertEquals("assertEquals", name);
  }

  @Test
  public void testImportWild() {
    context.importWild("com.demo.some.*");
    String name = context.importType("com.demo.some.Hello");

    assertEquals("Hello", name);

    JavaWriter writer = new JavaWriter(context, "");
    writer.emit(context);

    String code = writer.toString();

    assertFalse(code.contains("import com.demo.some.Hello;"));
    assertTrue(code.contains("import com.demo.some.*;"));
  }

  @Test
  public void testImportWildStatic() {
    context.importStaticWild("com.demo.some.Utils.*");
    String name = context.importStatic("com.demo.some.Utils.someHelper");

    assertEquals("someHelper", name);

    JavaWriter writer = new JavaWriter(context, "");
    writer.emit(context);

    String code = writer.toString();

    assertFalse(code.contains("import static com.demo.some.Utils.someHelper;"));
    assertTrue(code.contains("import static com.demo.some.Utils.*;"));
  }
}
