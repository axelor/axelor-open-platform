/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
