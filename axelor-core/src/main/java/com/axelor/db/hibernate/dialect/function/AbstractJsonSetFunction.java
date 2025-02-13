/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.db.hibernate.dialect.function;

import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.regex.Pattern;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.StandardBasicTypes;

public abstract class AbstractJsonSetFunction extends StandardSQLFunction {

  // Validates JSON path (e.g., "foo.bar.baz").
  // Possessive quantifiers (++) prevent catastrophic backtracking.
  private static final Pattern PATH_PATTERN = Pattern.compile("\\w++(\\.\\w++)*+");

  private final String transformValueFunction;

  protected AbstractJsonSetFunction(String name) {
    this(name, null);
  }

  protected AbstractJsonSetFunction(String name, String transformValueFunction) {
    super(name, StandardBasicTypes.STRING);
    this.transformValueFunction = transformValueFunction;
  }

  @Override
  public void render(
      SqlAppender sqlAppender,
      List<? extends SqlAstNode> sqlAstArguments,
      ReturnableType<?> returnType,
      SqlAstTranslator<?> translator) {

    if (sqlAstArguments.size() != 3) {
      throw new PersistenceException("Invalid use of 'json_set', requires 3 arguments.");
    }

    final SqlAstNode field = sqlAstArguments.getFirst();
    final SqlAstNode path = sqlAstArguments.get(1);
    final SqlAstNode value = sqlAstArguments.get(2);

    sqlAppender.appendSql(getName());
    sqlAppender.appendSql("(");
    translator.render(field, SqlAstNodeRenderingMode.DEFAULT);
    sqlAppender.appendSql(", ");

    if (path instanceof Literal literal) {
      final String pathName = literal.getLiteralValue().toString();
      validatePath(pathName);
      sqlAppender.appendSql(transformPath(pathName));
    } else {
      translator.render(path, SqlAstNodeRenderingMode.DEFAULT);
    }

    sqlAppender.appendSql(", ");

    if (transformValueFunction != null) {
      sqlAppender.appendSql(transformValueFunction);
      sqlAppender.appendSql("(");
    }
    translator.render(value, SqlAstNodeRenderingMode.DEFAULT);
    if (transformValueFunction != null) {
      sqlAppender.appendSql(")");
    }

    sqlAppender.appendSql(")");
  }

  private static void validatePath(String name) {
    if (!PATH_PATTERN.matcher(name).matches()) {
      throw new PersistenceException("Invalid json path: " + name);
    }
  }

  protected abstract String transformPath(String path);
}
