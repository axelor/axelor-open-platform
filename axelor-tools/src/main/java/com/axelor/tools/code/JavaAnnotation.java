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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** This class can be used to define annotations. */
public class JavaAnnotation implements JavaElement {

  private String name;

  private Map<String, List<JavaElement>> params = new LinkedHashMap<>();

  /**
   * Create new instance of {@link JavaAnnotation}.
   *
   * @param name fully qualified name of the annotation
   */
  public JavaAnnotation(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public JavaAnnotation param(String name, String format, Object... params) {
    if (format == null) {
      return this;
    }
    return param(name, new JavaCode(format, params));
  }

  public JavaAnnotation param(String name, JavaCode... value) {
    return param(name, Arrays.asList(value), x -> x);
  }

  public JavaAnnotation param(String name, JavaAnnotation... value) {
    return param(name, Arrays.asList(value), x -> x);
  }

  public JavaAnnotation param(String name, JavaParam value) {
    return null;
  }

  public <T> JavaAnnotation param(
      String name, Collection<T> value, Function<T, JavaElement> mapper) {
    if (value == null) {
      return this;
    }

    List<JavaElement> values =
        value.stream().map(mapper::apply).filter(Objects::nonNull).collect(Collectors.toList());

    if (values.isEmpty()) {
      return this;
    }

    params.computeIfAbsent(name, key -> new ArrayList<>()).addAll(values);

    return this;
  }

  private void emit(JavaWriter writer, List<JavaElement> values) {
    boolean block = values.size() > 1;

    if (block) {
      writer.emit("{").newLine().indent();
    }

    Iterator<JavaElement> iter = values.iterator();
    if (iter.hasNext()) {
      iter.next().emit(writer);
      iter.forEachRemaining(
          next -> {
            writer.emit(",");
            if (block) {
              writer.newLine();
            }
            next.emit(writer);
          });
    }

    if (block) {
      writer.newLine().unindent().emit("}");
    }
  }

  @Override
  public void emit(JavaWriter writer) {
    writer.emit("@").emit(writer.importType(name));
    if (params.isEmpty()) {
      return;
    }

    if (params.size() == 1 && params.containsKey("value")) {
      writer.emit("(");
      emit(writer, params.get("value"));
      writer.emit(")");
      return;
    }

    Iterator<Map.Entry<String, List<JavaElement>>> iter = params.entrySet().iterator();
    if (iter.hasNext()) {
      writer.emit("(").newLine().indent();
      Map.Entry<String, List<JavaElement>> first = iter.next();
      writer.emit(first.getKey()).emit(" = ");
      emit(writer, first.getValue());
      iter.forEachRemaining(
          next -> {
            writer.emit(",").newLine();
            writer.emit(next.getKey()).emit(" = ");
            emit(writer, next.getValue());
          });
      writer.newLine().unindent().emit(")");
    }
  }
}
