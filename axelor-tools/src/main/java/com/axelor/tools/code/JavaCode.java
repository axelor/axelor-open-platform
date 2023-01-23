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

import com.axelor.common.ObjectUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** This class is used to define java code. */
public class JavaCode implements JavaElement {

  private static final Pattern REPLACE_PATTERN = Pattern.compile("\\{(\\d+):(\\w)\\}");

  private final List<String> format = new ArrayList<>();

  private final List<Object[]> params = new ArrayList<>();

  /**
   * Create a new instance of {@link JavaCode}.
   *
   * <p>The format string accepts special positional placeholders to substitute with the given
   * params.
   *
   * <p>For example:
   *
   * <pre>
   * new JavaCode("private {0:t}<String> name = {0:t}.of({1:s}, {2:s});", "java.util.List", "Hello", "World");
   * </pre>
   *
   * This will output:
   *
   * <pre>
   * private List<String> name = List.of("Hello", "World");
   * </pre>
   *
   * Following placeholder suffixes are supported:
   *
   * <ul>
   *   <li><code>t</code> - for type, try to import it
   *   <li><code>s</code> - for string, quote it
   *   <li><code>m</code> - for member, try to import the class and emit SimpleName.member
   *   <li><code>M</code> - for member, try to static import it
   *   <li><code>l</code> - for literal
   * </ul>
   *
   * @param format the code string with positional placeholders
   * @param params the substitution parameters
   */
  public JavaCode(String format, Object... params) {
    next(format, params);
  }

  /**
   * Add next statement.
   *
   * @param format the code string with positional placeholders
   * @param params the substitution parameters
   * @return self
   */
  public JavaCode next(String format, Object... params) {
    this.format.add(format);
    this.params.add(params);
    return this;
  }

  private String replace(JavaWriter writer, Object[] params, MatchResult match) {
    int pos = Integer.valueOf(match.group(1));
    if (pos > params.length - 1) {
      return match.group();
    }
    String kind = match.group(2);
    Object param = params[pos];
    switch (kind) {
      case "t":
        return type(writer, param);
      case "m":
        return member(writer, param);
      case "M":
        return staticMember(writer, param);
      case "s":
        return text(writer, param);
      case "l":
        return text(param);
      case "a":
        return array(param);
      default:
        return match.group();
    }
  }

  private String text(Object value) {
    return String.valueOf(value);
  }

  private String array(Object value) {
    List<String> items =
        ObjectUtils.notEmpty(value)
            ? Arrays.asList(text(value).split("\\s*,\\s*"))
            : Collections.emptyList();
    return '{'
        + items.stream().map(item -> '"' + item + '"').collect(Collectors.joining(", "))
        + '}';
  }

  private String type(JavaWriter writer, Object value) {
    return writer.importType(text(value));
  }

  private String member(JavaWriter writer, Object value) {
    String fqn = text(value);
    if (fqn == null || fqn.isBlank() || fqn.indexOf('.') == -1) {
      return fqn;
    }

    String full = fqn.substring(0, fqn.lastIndexOf('.'));
    String name = fqn.substring(full.length());
    String type = writer.importType(full);

    return type + name;
  }

  private String staticMember(JavaWriter writer, Object value) {
    String fqn = text(value);
    if (fqn == null || fqn.isBlank() || fqn.indexOf('.') == -1) {
      return fqn;
    }
    return writer.importStatic(fqn);
  }

  private String text(JavaWriter writer, Object value) {
    if (value == null) {
      return "";
    }
    return '"' + text(value) + '"';
  }

  @Override
  public void emit(JavaWriter writer) {
    for (int i = 0; i < format.size(); i++) {
      String format = this.format.get(i);
      Object[] params = this.params.get(i);
      Matcher matcher = REPLACE_PATTERN.matcher(format);
      try {
        String code = matcher.replaceAll(m -> this.replace(writer, params, m));
        writer.emit(code);
      } catch (NullPointerException e) {
        matcher.replaceAll(m -> this.replace(writer, params, m));
        throw new RuntimeException(e);
      }
    }
  }
}
