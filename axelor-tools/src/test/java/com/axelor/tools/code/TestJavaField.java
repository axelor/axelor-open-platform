/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
    JavaAnnotation annotation = new JavaAnnotation("jakarta.validation.constraints.NotNull");
    field.annotation(annotation);
    String code = writer.emit(field).toString();
    assertTrue(code.contains("@NotNull"));
  }
}
