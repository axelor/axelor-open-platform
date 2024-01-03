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
