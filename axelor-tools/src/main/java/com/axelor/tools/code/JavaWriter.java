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

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class provides code writing functionality.
 *
 * <p>It provides methods to emit code and automatically handles source code indentation.
 */
public final class JavaWriter {

  private static final String NL = "\n";

  private JavaContext context;

  private StringBuilder content;

  private String indentWith;

  private int indent = 0;

  private boolean newLine;

  /**
   * Create a new instance of {@link JavaWriter} with the given import context and indent string.
   *
   * @param context the import context
   * @param indentWith indent string
   */
  public JavaWriter(JavaContext context, String indentWith) {
    this.context = context;
    this.content = new StringBuilder();
    this.indentWith = indentWith;
  }

  public void importStaticWild(String wild) {
    context.importStaticWild(wild);
  }

  public void importWild(String wild) {
    context.importWild(wild);
  }

  public String importStatic(String name) {
    return context.importStatic(name);
  }

  public String importType(String name) {
    return context.importType(name);
  }

  /**
   * Indent one time.
   *
   * @return self
   */
  public JavaWriter indent() {
    this.indent = Math.max(0, indent + 1);
    return this;
  }

  /**
   * Unindent one time.
   *
   * @return self
   */
  public JavaWriter unindent() {
    this.indent = Math.max(0, indent - 1);
    return this;
  }

  /**
   * Emit a new line.
   *
   * @return self
   */
  public JavaWriter newLine() {
    return newLine(1);
  }

  /**
   * Emit given numbers of new lines.
   *
   * @param n number of new lines
   * @return self
   */
  public JavaWriter newLine(int n) {
    content.append(NL.repeat(n));
    newLine = true;
    return this;
  }

  /**
   * Emit the given modifiers.
   *
   * @param modifiers the modifiers
   * @return self
   */
  public JavaWriter emit(int modifiers) {
    if (modifiers > 0) {
      this.emit(Modifier.toString(modifiers));
    }
    return this;
  }

  /**
   * Emit the given code.
   *
   * @param code the code string
   * @return self
   */
  public JavaWriter emit(String code) {
    if (code == null) {
      return this;
    }

    String lead = indentWith.repeat(indent);
    String[] lines = code.split(NL, -1);

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (i > 0) newLine();
      if (line.isEmpty()) {
        continue;
      }
      if (newLine) content.append(lead);
      content.append(line);
      newLine = false;
    }

    return this;
  }

  /**
   * Emit code for the given element.
   *
   * @param element the element to emit
   * @return self
   */
  public JavaWriter emit(JavaElement element) {
    element.emit(this);
    return this;
  }

  /**
   * Emit code for the given elements joined by the given separator.
   *
   * @param elements the elements to emit code for
   * @param separator separator string to join the element code
   * @return self
   */
  public JavaWriter emit(Collection<? extends JavaElement> elements, String separator) {
    if (elements == null) return this;
    Iterator<? extends JavaElement> iter = elements.iterator();
    if (iter.hasNext()) {
      emit(iter.next());
      iter.forEachRemaining(
          element -> {
            emit(separator);
            emit(element);
          });
    }
    return this;
  }

  @Override
  public String toString() {
    return content.toString();
  }
}
