/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestJavaField {

  private JavaContext context;

  private JavaWriter writer;

  @BeforeEach
  public void beforeEach() {
    context = new JavaContext("com.axelor.some");
    writer = new JavaWriter(context, "  ");
  }

  @Test
  public void testBasic() {
    JavaField field =
        new JavaField("person", "com.axelor.some.Person", Modifier.PRIVATE | Modifier.FINAL);

    String code = writer.emit(field).toString();
    assertEquals("private final Person person;", code);
  }

  @Test
  public void testDefaultValueString() {
    JavaField field =
        new JavaField("MESSAGE", "String", Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL)
            .defaultValue(new JavaCode("{0:s}", "Hello..."));

    String code = writer.emit(field).toString();
    assertEquals("public static final String MESSAGE = \"Hello...\";", code);
  }

  @Test
  public void testDefaultValueBoolean() {
    JavaField field =
        new JavaField("active", "bool", Modifier.PRIVATE)
            .defaultValue(new JavaCode("{0:l}", "true"));

    String code = writer.emit(field).toString();
    assertEquals("private bool active = true;", code);
  }

  @Test
  public void testDefaultValueMember() {
    JavaField field =
        new JavaField("date", "LocalDate", Modifier.PRIVATE)
            .defaultValue(new JavaCode("{0:t}.now()", "java.date.LocalDate"));

    String code = writer.emit(field).toString();
    assertEquals("private LocalDate date = LocalDate.now();", code);
  }

  @Test
  public void testDefaultValueNew() {
    JavaField field =
        new JavaField("person", "Person", Modifier.PRIVATE)
            .defaultValue(new JavaCode("new {0:t}()", "com.some.example.Person"));

    String code = writer.emit(field).toString();
    assertEquals("private Person person = new Person();", code);
  }

  @Test
  public void testJavaDoc() {
    JavaField field = new JavaField("message", "String", Modifier.PRIVATE);
    field.doc(new JavaDoc("Say what you want..."));
    String code = writer.emit(field).toString();
    assertTrue(code.contains("/**"));
    assertTrue(code.contains(" * Say what you want..."));
    assertTrue(code.contains(" */"));
  }

  @Test
  public void testAnnotation() {
    JavaField field = new JavaField("message", "String", Modifier.PRIVATE);
    JavaAnnotation annotation = new JavaAnnotation("javax.validation.constraints.NotNull");
    field.annotation(annotation);
    String code = writer.emit(field).toString();
    assertTrue(code.contains("@NotNull"));
  }
}
