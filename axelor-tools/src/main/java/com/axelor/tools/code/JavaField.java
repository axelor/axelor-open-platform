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

/** This class can be used to define class fields. */
public class JavaField extends JavaAnnotable<JavaField> {

  private JavaCode defaultValue;

  /**
   * Create a new instance of the {@link JavaField} with given name and type.
   *
   * @param name the field name
   * @param type the field type
   * @param modifiers field modifiers
   */
  public JavaField(String name, String type, int... modifiers) {
    super(name, type, modifiers);
  }

  /**
   * Define the default value
   *
   * @param defaultValue the default value code
   * @return self
   */
  public JavaField defaultValue(JavaCode defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  public String getGetterName() {
    return "boolean".equals(getType())
        ? "is" + JavaCodeUtils.firstUpper(getName())
        : "get" + JavaCodeUtils.firstUpper(getName());
  }

  public String getSetterName() {
    return "set" + JavaCodeUtils.firstUpper(getName());
  }

  public JavaMethod getGetterMethod() {
    JavaMethod method = new JavaMethod(getGetterName(), getType(), Modifier.PUBLIC);
    method.code("return {0:l};", getName());
    return method;
  }

  public JavaMethod getSetterMethod() {
    JavaMethod method = new JavaMethod(getSetterName(), "void", Modifier.PUBLIC);
    method.param(getName(), getType());
    method.code("this.{0:l} = {0:l};", getName());
    return method;
  }

  @Override
  public void emit(JavaWriter writer) {
    super.emit(writer);

    if (defaultValue != null) {
      writer.emit(" = ").emit(defaultValue);
    }

    writer.emit(";");
  }
}
