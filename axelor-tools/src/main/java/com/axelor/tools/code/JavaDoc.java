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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** This class can be used to define java doc for a java construct. */
public class JavaDoc implements JavaElement {

  private List<JavaCode> lines = new ArrayList<>();

  private Map<String, JavaCode> params = new LinkedHashMap<>();

  private List<JavaCode> exceptions = new ArrayList<>();

  private JavaCode returns;

  /**
   * Create a new instance of the {@link JavaDoc} with the given subject line.
   *
   * @param format the doc subject line as {@link JavaCode} format
   * @param params the code format params
   */
  public JavaDoc(String format, Object... params) {
    this.line(format, params);
  }

  /**
   * Add a line to the doc.
   *
   * @param format the doc subject line as {@link JavaCode} format
   * @param params the code format params
   * @return self
   */
  public JavaDoc line(String format, Object... params) {
    this.lines.add(new JavaCode(format, params));
    return this;
  }

  /**
   * Add a <code>@param</code> tag.
   *
   * @param name the name of the param
   * @param format the doc subject line as {@link JavaCode} format
   * @param params the code format params
   * @return self
   */
  public JavaDoc param(String name, String format, Object... params) {
    this.params.put(name, new JavaCode(format, params));
    return this;
  }

  /**
   * Add an exception to the doc.
   *
   * @param type the exception type name
   * @return self
   */
  public JavaDoc exception(String type) {
    this.exceptions.add(new JavaCode("{0:t}", type));
    return this;
  }

  /**
   * Add <code>@return</code> clause.
   *
   * @param format the doc subject line as {@link JavaCode} format
   * @param params the code format params
   * @return self
   */
  public JavaDoc returns(String format, Object... params) {
    this.returns = new JavaCode(format, params);
    return this;
  }

  @Override
  public void emit(JavaWriter writer) {
    writer.emit("/**").newLine();

    Iterator<JavaCode> iter = lines.iterator();
    if (iter.hasNext()) {
      writer.emit(" * ").emit(iter.next()).newLine();
      if (iter.hasNext()) {
        writer.emit(" *").newLine();
      }
      iter.forEachRemaining(code -> writer.emit(" * ").emit(code).newLine());
    }

    if (!params.isEmpty()) {
      writer.emit(" *").newLine();
      for (Map.Entry<String, JavaCode> item : params.entrySet()) {
        writer.emit(" * @param ").emit(item.getKey()).emit(" ").emit(item.getValue()).newLine();
      }
    }

    if (!exceptions.isEmpty()) {
      writer.emit(" *").newLine();
      exceptions.forEach(code -> writer.emit(" * @throws ").emit(code).newLine());
    }

    if (returns != null) {
      writer.emit(" *").newLine();
      writer.emit(" * @return ").emit(returns).newLine();
    }

    writer.emit(" */").newLine();
  }
}
