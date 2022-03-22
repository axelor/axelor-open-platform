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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** This class can be used to define java methods and constructors. */
public class JavaMethod extends JavaAnnotable<JavaMethod> {

  private final List<JavaParam> params = new ArrayList<>();

  private final List<JavaCode> throwables = new ArrayList<>();

  private final List<JavaCode> code = new ArrayList<>();

  private boolean defaultMethod;

  private boolean declaration;

  /**
   * Create a new instance of {@link JavaMethod} with the given name, type and modifiers.
   *
   * @param name name of the method
   * @param type type of the method, pass <code>null</code> for constructor
   * @param modifiers method modifiers
   * @see Modifier
   */
  public JavaMethod(String name, String type, int... modifiers) {
    super(name, type, modifiers);
  }

  /**
   * Add method parameter.
   *
   * @param name the parameter name
   * @param type the parameter type
   * @return self
   */
  public JavaMethod param(String name, String type) {
    this.params.add(new JavaParam(name, type));
    return this;
  }

  /**
   * Add method parameter.
   *
   * @param param the parameter to add
   * @return self
   */
  public JavaMethod param(JavaParam param) {
    this.params.add(param);
    return this;
  }

  /**
   * Add throwables this method can throw.
   *
   * @param type name of the throwable type
   * @return self
   */
  public JavaMethod throwable(String type) {
    this.throwables.add(new JavaCode("{0:t}", type));
    return this;
  }

  /**
   * Set the method as interface default method.
   *
   * @param defaultMethod whether the method is a default method
   * @return self
   */
  public JavaMethod defaultMethod(boolean defaultMethod) {
    this.defaultMethod = defaultMethod;
    return this;
  }

  /**
   * Set the method as declaration only (for interface type).
   *
   * @param declaration whether the method is declaration only
   * @return self
   */
  public JavaMethod declaration(boolean declaration) {
    this.declaration = declaration;
    return this;
  }

  /**
   * Add code for method body.
   *
   * @param code the code
   * @return self
   */
  public JavaMethod code(JavaCode code) {
    this.code.add(code);
    return this;
  }

  /**
   * Add code for method body.
   *
   * @param format the code string
   * @param args arguments for the format specifiers
   * @return self
   */
  public JavaMethod code(String format, Object... args) {
    this.code.add(new JavaCode(format, args));
    return this;
  }

  /**
   * Add code for the method body.
   *
   * @param code the code
   * @return self
   */
  public JavaMethod code(Collection<JavaCode> code) {
    this.code.addAll(code);
    return this;
  }

  @Override
  protected void emitModifiers(JavaWriter writer) {
    super.emitModifiers(writer);
    if (defaultMethod) {
      writer.emit(" default ");
    }
  }

  @Override
  public void emit(JavaWriter writer) {
    super.emit(writer);
    writer.emit("(");

    if (params.stream().anyMatch(p -> !p.getAnnotations().isEmpty())) {
      writer.newLine().indent();
      writer.emit(params, ",\n");
      writer.newLine().unindent();
    } else {
      writer.emit(params, ", ");
    }

    writer.emit(")");

    if (!throwables.isEmpty()) {
      writer.emit(" throws ").emit(throwables, ", ");
    }

    if (Modifier.isAbstract(getModifiers()) || declaration) {
      writer.emit(";").newLine();
      return;
    }

    writer.emit(" {").newLine().indent();
    if (!code.isEmpty()) {
      writer.emit(code, "\n");
      writer.newLine();
    }
    writer.unindent().emit("}").newLine();
  }
}
