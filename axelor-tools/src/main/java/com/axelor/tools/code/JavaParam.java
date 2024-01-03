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

import java.lang.reflect.Modifier;

/** This class can be used to define java method params. */
public class JavaParam extends JavaAnnotable<JavaParam> {

  /**
   * Create a new instance of {@link JavaParam}.
   *
   * @param name the parameter name
   * @param type the parameter type
   */
  public JavaParam(String name, String type) {
    super(name, type);
  }

  /**
   * Create a new instance of {@link JavaParam}.
   *
   * @param name the parameter name
   * @param type the parameter type
   * @param isFinal whether the param is final
   */
  public JavaParam(String name, String type, boolean isFinal) {
    super(name, type, Modifier.FINAL);
  }
}
