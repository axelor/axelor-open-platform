/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code.entity.model;

import java.util.Set;

public class Naming {
  /** Check whether the given name is a reserved keyword. */
  public static boolean isReserved(String name) {
    return RESERVED_JAVA.contains(name) || RESERVED_EXTRA.contains(name);
  }

  /** Check whether the given name is SQL reserved keyword. */
  public static boolean isKeyword(String name) {
    return RESERVED_POSTGRESQL.contains(name);
  }

  /** Quote the given column name. */
  public static String quoteColumn(String name) {
    return "`" + name + "`";
  }

  // Internal Reserved
  private static final Set<String> RESERVED_EXTRA =
      Set.of("version", "archived", "selected", "constructor");

  // Java Keywords
  private static final Set<String> RESERVED_JAVA =
      Set.of(
          "abstract",
          "assert",
          "boolean",
          "break",
          "byte",
          "case",
          "catch",
          "char",
          "class",
          "const",
          "continue",
          "default",
          "do",
          "double",
          "else",
          "enum",
          "extends",
          "final",
          "finally",
          "float",
          "for",
          "goto",
          "if",
          "implements",
          "import",
          "instanceof",
          "int",
          "interface",
          "long",
          "native",
          "new",
          "package",
          "private",
          "protected",
          "public",
          "return",
          "short",
          "static",
          "strictfp",
          "super",
          "switch",
          "synchronized",
          "this",
          "throw",
          "throws",
          "transient",
          "try",
          "void",
          "volatile",
          "while");

  // PostgreSQL reserved keywords (up to PostgreSQL 18)
  private static final Set<String> RESERVED_POSTGRESQL =
      Set.of(
          "all",
          "analyse",
          "analyze",
          "and",
          "any",
          "array",
          "as",
          "asc",
          "asymmetric",
          "authorization",
          "binary",
          "both",
          "case",
          "cast",
          "check",
          "collate",
          "collation",
          "column",
          "concurrently",
          "constraint",
          "create",
          "cross",
          "current_catalog",
          "current_date",
          "current_role",
          "current_schema",
          "current_time",
          "current_timestamp",
          "current_user",
          "default",
          "deferrable",
          "desc",
          "distinct",
          "do",
          "else",
          "end",
          "except",
          "false",
          "fetch",
          "for",
          "foreign",
          "freeze",
          "from",
          "full",
          "grant",
          "group",
          "having",
          "ilike",
          "in",
          "initially",
          "inner",
          "intersect",
          "into",
          "is",
          "isnull",
          "join",
          "lateral",
          "leading",
          "left",
          "like",
          "limit",
          "localtime",
          "localtimestamp",
          "natural",
          "not",
          "notnull",
          "null",
          "offset",
          "on",
          "only",
          "or",
          "order",
          "outer",
          "overlaps",
          "placing",
          "primary",
          "references",
          "returning",
          "right",
          "select",
          "session_user",
          "similar",
          "some",
          "symmetric",
          "table",
          "tablesample",
          "then",
          "to",
          "trailing",
          "true",
          "union",
          "unique",
          "user",
          "using",
          "variadic",
          "verbose",
          "when",
          "where",
          "window",
          "with");
}
