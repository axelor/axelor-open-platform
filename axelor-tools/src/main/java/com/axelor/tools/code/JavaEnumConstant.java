/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** This class can be used to define enum constants. */
public class JavaEnumConstant extends JavaAnnotable<JavaEnumConstant> {

  private List<JavaCode> args;

  /**
   * Create a new instance of the {@link JavaEnumConstant}.
   *
   * @param name the constant name
   * @param args the arguments
   */
  public JavaEnumConstant(String name, Object... args) {
    super(name, null);
    this.args = Stream.of(args).map(arg -> new JavaCode("{0:l}", arg)).collect(Collectors.toList());
  }

  /**
   * Add an argument.
   *
   * @param arg the argument
   * @return self
   */
  public JavaEnumConstant arg(JavaCode arg) {
    this.args.add(arg);
    return this;
  }

  @Override
  public void emit(JavaWriter writer) {
    super.emit(writer);

    if (!args.isEmpty()) {
      writer.emit("(");
      writer.emit(args, ", ");
      writer.emit(")");
    }
  }
}
