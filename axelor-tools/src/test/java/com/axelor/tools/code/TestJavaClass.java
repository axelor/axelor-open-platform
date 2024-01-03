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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestJavaClass {

  private JavaContext context;

  private JavaWriter writer;

  @BeforeEach
  public void beforeEach() {
    context = new JavaContext("com.axelor.some");
    writer = new JavaWriter(context, "  ");
  }

  @Test
  public void testBasic() {
    JavaType pojo = JavaType.newClass("Hello", Modifier.PUBLIC | Modifier.FINAL);
    String code = writer.emit(pojo).toString();
    assertEquals("public final class Hello {}", code.replaceAll("\n", ""));
  }

  @Test
  public void testSuperTypes() {
    JavaType pojo = JavaType.newClass("Hello", Modifier.PUBLIC | Modifier.ABSTRACT);
    pojo.superType("com.example.some.SuperType");
    pojo.superInterface("com.example.some.IType");
    pojo.superInterface("java.io.Serializable");
    String code = writer.emit(pojo).toString();
    assertEquals(
        "public abstract class Hello extends SuperType implements IType, Serializable {}",
        code.replaceAll("\n", ""));
  }

  @Test
  public void testMembers() {
    JavaType pojo = JavaType.newClass("Person", Modifier.PUBLIC);
    JavaField firstName = new JavaField("firstName", "String", Modifier.PRIVATE);
    JavaField lastName = new JavaField("lastName", "String", Modifier.PRIVATE);

    JavaMethod ctor = new JavaMethod("Person", null, Modifier.PUBLIC);
    JavaMethod getFirstName = new JavaMethod("getFirstName", "String", Modifier.PUBLIC);
    JavaMethod getLastName = new JavaMethod("getLastName", "String", Modifier.PUBLIC);

    getFirstName.code("return firstName;");
    getLastName.code("return lastName;");

    pojo.field(firstName)
        .field(lastName)
        .constructor(ctor)
        .method(getFirstName)
        .method(getLastName);

    String[] lines = writer.emit(pojo).toString().split("\n");

    String[] expecteds = {
      "public class Person {",
      "",
      "  private String firstName;",
      "",
      "  private String lastName;",
      "",
      "  public Person() {",
      "  }",
      "",
      "  public String getFirstName() {",
      "    return firstName;",
      "  }",
      "",
      "  public String getLastName() {",
      "    return lastName;",
      "  }",
      "}"
    };

    assertArrayEquals(expecteds, lines);
  }

  @Test
  public void testJavaDoc() {
    JavaType pojo = JavaType.newClass("Person", Modifier.PUBLIC);
    pojo.doc(new JavaDoc("This is an entity class..."));
    String code = writer.emit(pojo).toString();
    assertTrue(code.contains("/**"));
    assertTrue(code.contains(" * This is an entity class..."));
    assertTrue(code.contains(" */"));
  }

  @Test
  public void testAnnotation() {
    JavaType pojo = JavaType.newClass("Person", Modifier.PUBLIC);
    JavaAnnotation annotation = new JavaAnnotation("javax.persistence.Entity");
    pojo.annotation(annotation);
    String code = writer.emit(pojo).toString();
    assertTrue(code.contains("@Entity"));
  }

  @Test
  public void testRawCode() {
    JavaType pojo = JavaType.newClass("Person", Modifier.PUBLIC);
    JavaContext ctx = new JavaContext("com.some.example");
    JavaFile file = new JavaFile(ctx, pojo);

    pojo.rawImports("import java.util.*;", "import static java.util.Objects.*;");

    pojo.rawCode("private String name;\n");
    pojo.rawCode("\n");
    pojo.rawCode("public setName(String name) {\n");
    pojo.rawCode("  this.name = requireNonNull(name);\n");
    pojo.rawCode("}");

    pojo.field("names", "java.util.List<String>", Modifier.PRIVATE);

    pojo.field("code", "String", Modifier.PRIVATE);
    pojo.method(
        new JavaMethod("setCode", "void", Modifier.PUBLIC)
            .param("code", "String")
            .code("this.code = {0:M}(code)", "java.util.Objects.requireNonNull"));

    StringBuilder builder = new StringBuilder();
    try {
      file.writeTo(builder);
    } catch (IOException e) {
    }

    String code = builder.toString();

    assertTrue(code.contains("import static java.util.Objects.*;"));
    assertFalse(code.contains("import static java.util.Objects.requireNonNull;"));

    assertTrue(code.contains("import java.util.*;"));
    assertFalse(code.contains("import java.util.List;"));
  }
}
