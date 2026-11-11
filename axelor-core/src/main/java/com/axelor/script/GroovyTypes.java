/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;

/**
 * The rules shared by the compile-time checks on the classes named in a script ({@link
 * GroovyImportCheck}, {@link GroovyTypeCheck}, {@link GroovyClassCheck}): which class a type node
 * refers to, and when there is nothing to check.
 */
final class GroovyTypes {

  private GroovyTypes() {}

  /**
   * The existing class the type refers to, to be checked against the policy, or {@code null} when
   * there is nothing to check: no type, a dynamic ({@code def}) or primitive type, a generic
   * placeholder, or a class declared in the script itself (not compiled yet, and checked on its
   * own). An array is resolved to its element type.
   */
  static Class<?> resolve(ClassNode type) {
    if (type == null) {
      return null;
    }
    ClassNode node = type;
    while (node.isArray()) {
      node = node.getComponentType();
    }
    if (ClassHelper.isDynamicTyped(node)
        || ClassHelper.isPrimitiveType(node)
        || node.isGenericsPlaceHolder()
        || node.isPrimaryClassNode()
        || !node.isResolved()) {
      return null;
    }
    return node.getTypeClass();
  }
}
