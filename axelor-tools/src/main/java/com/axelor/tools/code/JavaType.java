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

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** This class can be used to define Java class. */
public class JavaType extends JavaAnnotable<JavaType> {

  private static final Pattern IMPORT_STATEMENT_PATTERN =
      Pattern.compile("(import\\s+)(static\\s+)?(.*)");

  private JavaCode superType;

  private final Kind kind;

  private final List<JavaCode> superInterfaces = new ArrayList<>();

  private final List<JavaField> fields = new ArrayList<>();

  private final List<JavaMethod> methods = new ArrayList<>();

  private final List<JavaMethod> constructors = new ArrayList<>();

  private final List<JavaEnumConstant> enumConstants = new ArrayList<>();

  private final List<String> rawImports = new ArrayList<>();

  private final List<String> rawCode = new ArrayList<>();

  private JavaType(String name, Kind kind, int... modifiers) {
    super(name, kind.name().toLowerCase(), modifiers);
    this.kind = kind;
  }

  public static JavaType newInterface(String name, int... modifiers) {
    return new JavaType(name, Kind.INTERFACE, modifiers);
  }

  public static JavaType newClass(String name, int... modifiers) {
    return new JavaType(name, Kind.CLASS, modifiers);
  }

  public static JavaType newEnum(String name, int... modifiers) {
    return new JavaType(name, Kind.ENUM, modifiers);
  }

  /**
   * Set the super class.
   *
   * @param type the super class
   * @return self
   */
  public JavaType superType(String type) {
    this.superType = new JavaCode("{0:t}", type);
    return this;
  }

  /**
   * Add an interface to implement.
   *
   * @param type the interface type
   * @return self
   */
  public JavaType superInterface(String type) {
    this.superInterfaces.add(new JavaCode("{0:t}", type));
    return this;
  }

  /**
   * Add a field.
   *
   * @param name name of the field
   * @param type the type of the field
   * @param modifiers modifiers
   * @return self
   */
  public JavaType field(String name, String type, int... modifiers) {
    return field(new JavaField(name, type, modifiers));
  }

  /**
   * Add a field.
   *
   * @param field the field to add
   * @return self
   */
  public JavaType field(JavaField field) {
    this.fields.add(field);
    return this;
  }

  /**
   * Add an enum constant.
   *
   * @param name the constant to add
   * @param args enum arguments
   * @return self
   */
  public JavaType enumConstant(String name, Object... args) {
    enumConstants.add(new JavaEnumConstant(name, args));
    return this;
  }

  /**
   * Add an enum constant.
   *
   * @param constant the constant to add
   * @return self
   */
  public JavaType enumConstant(JavaEnumConstant constant) {
    enumConstants.add(constant);
    return this;
  }

  /**
   * Add a method.
   *
   * @param method the method to add
   * @return self
   */
  public JavaType method(JavaMethod method) {
    this.methods.add(method);
    return this;
  }

  /**
   * Add a constructor.
   *
   * @param constructor the constructor to add
   * @return self
   */
  public JavaType constructor(JavaMethod constructor) {
    this.constructors.add(constructor);
    return this;
  }

  /**
   * Add raw import statements.
   *
   * @param imports the import statements.
   * @return self
   */
  public JavaType rawImports(String... imports) {
    Collections.addAll(rawImports, imports);
    return this;
  }

  /**
   * Add raw code.
   *
   * @param code the code to add
   * @return self
   */
  public JavaType rawCode(String code) {
    this.rawCode.add(code);
    return this;
  }

  private void processRawImports(JavaWriter writer) {
    rawImports.stream()
        .map(String::trim)
        .map(IMPORT_STATEMENT_PATTERN::matcher)
        .filter(Matcher::matches)
        .forEach(
            matcher -> {
              String s = matcher.group(2);
              String n = matcher.group(3).trim();
              if (n.endsWith(";")) n = n.substring(0, n.length() - 1);
              if (n.endsWith(".*")) {
                if (s == null) writer.importWild(n);
                if (s != null) writer.importStaticWild(n);
              } else {
                if (s == null) writer.importType(n);
                if (s != null) writer.importStatic(n);
              }
            });
  }

  @Override
  public void emit(JavaWriter writer) {
    this.processRawImports(writer);
    super.emit(writer);

    if (superType != null) {
      writer.emit(" extends ").emit(superType);
    }

    if (!superInterfaces.isEmpty()) {
      writer
          .emit(" ")
          .emit(kind == Kind.INTERFACE ? "extends" : "implements")
          .emit(" ")
          .emit(superInterfaces, ", ");
    }

    writer.emit(" {").newLine().indent();

    if (!enumConstants.isEmpty()) {
      writer.emit(enumConstants, ",\n\n");

      if (Stream.of(fields, constructors, methods, rawCode).anyMatch(not(List::isEmpty))) {
        writer.emit(";");
      }

      writer.newLine();
    }

    if (!fields.isEmpty()) {
      writer.newLine();
      writer.emit(fields, "\n\n");
      writer.newLine();
    }

    if (!constructors.isEmpty()) {
      writer.newLine();
      writer.emit(constructors, "\n");
    }

    if (!methods.isEmpty()) {
      writer.newLine();
      writer.emit(methods, "\n");
    }

    // emit raw code
    if (!rawCode.isEmpty()) {
      writer.newLine();
      rawCode.forEach(writer::emit);
      writer.newLine();
    }

    writer.unindent().emit("}").newLine();
  }

  static enum Kind {
    INTERFACE,
    CLASS,
    ENUM;
  }
}
