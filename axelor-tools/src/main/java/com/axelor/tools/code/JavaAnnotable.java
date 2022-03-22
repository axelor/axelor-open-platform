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
import java.util.List;
import java.util.stream.IntStream;

public abstract class JavaAnnotable<T extends JavaAnnotable<T>> implements JavaElement {

  private List<JavaAnnotation> annotations = new ArrayList<>();

  private JavaDoc doc;

  private int modifiers;

  private final String name;

  private final String type;

  public JavaAnnotable(String name, String type, int... modifiers) {
    this.name = name;
    this.type = type;
    this.modifiers = IntStream.of(modifiers).reduce(0, (x, y) -> x | y);
  }

  @SuppressWarnings("unchecked")
  protected T self() {
    return (T) this;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public int getModifiers() {
    return modifiers;
  }

  public List<JavaAnnotation> getAnnotations() {
    return annotations;
  }

  /**
   * Set java doc.
   *
   * @param doc the javadoc
   * @return self
   */
  public T doc(JavaDoc doc) {
    this.doc = doc;
    return self();
  }

  /**
   * Add annotation.
   *
   * @param annotation the annotation to add
   * @return self
   */
  public T annotation(JavaAnnotation annotation) {
    this.annotations.add(annotation);
    return self();
  }

  /**
   * Set new modifiers.
   *
   * @param modifiers the modifiers to set
   * @return self
   */
  public T modifiers(int... modifiers) {
    this.modifiers = IntStream.of(modifiers).reduce(0, (x, y) -> x | y);
    return self();
  }

  protected void emitAnnotations(JavaWriter writer) {
    annotations.forEach(a -> writer.emit(a).newLine());
  }

  protected void emitModifiers(JavaWriter writer) {
    if (modifiers > 0) {
      writer.emit(modifiers).emit(" ");
    }
  }

  @Override
  public void emit(JavaWriter writer) {
    if (doc != null) {
      writer.emit(doc);
    }

    emitAnnotations(writer);
    emitModifiers(writer);

    if (type != null) {
      writer.emit(writer.importType(type)).emit(" ");
    }

    writer.emit(name);
  }
}
