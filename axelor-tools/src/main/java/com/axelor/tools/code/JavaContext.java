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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** This class manages import statements in the context of given package name. */
public class JavaContext implements JavaElement {

  private static final Pattern NAME_PATTERN =
      Pattern.compile(
          "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");

  private String packageName;

  private List<String> wildStatics = new ArrayList<>();

  private List<String> wildImports = new ArrayList<>();

  private Map<String, String> statics = new HashMap<>();

  private Map<String, String> imports = new HashMap<>();

  /**
   * Create a new instance for the given package name.
   *
   * <p>All direct imports under the give package name will be ignored.
   *
   * @param packageName the package name
   */
  public JavaContext(String packageName) {
    this.packageName = packageName;
  }

  /**
   * Get package name of the current import context.
   *
   * @return the package name
   */
  public String getPackageName() {
    return packageName;
  }

  /**
   * Try static import of the given fully qualified name.
   *
   * @param name the static name to import
   * @return this
   */
  public String importStatic(String name) {
    return notWildThen(name, s -> importSingle(s, statics));
  }

  /**
   * Try importing the given fully qualified name.
   *
   * @param type the name to import
   * @return this
   */
  public String importType(String type) {
    return notWildThen(
        type, s -> NAME_PATTERN.matcher(s).replaceAll(m -> importSingle(m.group(), imports)));
  }

  /**
   * Add a wildcard static import.
   *
   * @param wild the wild card
   */
  public void importStaticWild(String wild) {
    String name = wild.trim();
    if (name.endsWith(";")) name = name.substring(0, name.length() - 1);
    if (name.endsWith(".*")) name = name.substring(0, name.length() - 2);
    this.wildStatics.add(name);
  }

  /**
   * Add a wildcard import.
   *
   * @param wild the wild card
   */
  public void importWild(String wild) {
    String name = wild.trim();
    if (name.endsWith(";")) name = name.substring(0, name.length() - 1);
    if (name.endsWith(".*")) name = name.substring(0, name.length() - 2);
    this.wildImports.add(name);
  }

  private List<String> getStaticStatements() {
    return statics.values().stream()
        .filter(s -> shouldImportStatic(s))
        .map(s -> String.format("import static %s;", s))
        .collect(toList());
  }

  private List<String> getImportStatements() {
    return imports.values().stream()
        .filter(s -> shouldImport(s))
        .map(s -> String.format("import %s;", s))
        .collect(toList());
  }

  private String notWildThen(String fqn, Function<String, String> map) {
    if (fqn == null) return null;
    if (fqn.endsWith(".*")) {
      throw new IllegalArgumentException("can't handle wildcard import");
    }
    return map.apply(fqn);
  }

  private String importSingle(String fqn, Map<String, String> imports) {
    final String[] parts = fqn.split("\\.");
    final String name = parts[parts.length - 1];
    if (parts.length == 1) {
      return fqn;
    }
    imports.putIfAbsent(name, fqn);
    return fqn.equals(imports.get(name)) ? name : fqn;
  }

  private boolean shouldImportStatic(String type) {
    String pkg = type.substring(0, type.lastIndexOf('.'));
    return !wildStatics.contains(pkg);
  }

  private boolean shouldImport(String type) {
    String pkg = type.substring(0, type.lastIndexOf('.'));
    return !pkg.equals("java.lang") && !pkg.equals(packageName) && !wildImports.contains(pkg);
  }

  private List<String> sortImports(List<String> imports) {
    return imports.stream().distinct().sorted().collect(Collectors.toList());
  }

  @Override
  public void emit(JavaWriter writer) {
    writer.emit("package ").emit(packageName).emit(";").newLine(2);

    List<String> statics = sortImports(getStaticStatements());
    List<String> imports = sortImports(getImportStatements());

    if (!wildStatics.isEmpty()) {
      sortImports(wildStatics).forEach(s -> writer.emit("import static " + s + ".*;").newLine());
      writer.newLine();
    }

    if (!wildImports.isEmpty()) {
      sortImports(wildImports).forEach(s -> writer.emit("import " + s + ".*;").newLine());
      writer.newLine();
    }

    if (!statics.isEmpty()) {
      statics.forEach(s -> writer.emit(s).newLine());
      writer.newLine();
    }

    if (!imports.isEmpty()) {
      imports.forEach(s -> writer.emit(s).newLine());
      writer.newLine();
    }
  }
}
