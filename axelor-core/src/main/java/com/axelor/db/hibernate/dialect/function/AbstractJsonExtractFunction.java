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
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.BasicTypeReference;

public abstract class AbstractJsonExtractFunction extends StandardSQLFunction {

  // Validates JSON path argument when literals are used.
  private static final Pattern ARGS_PATTERN = Pattern.compile("\\w+");

  private final String cast;
  private final String transformFunction;
  private final Collector<CharSequence, ?, String> collectPath;

  protected AbstractJsonExtractFunction(String name, BasicTypeReference<?> type, String cast) {
    this(name, type, cast, null, null);
  }

  protected AbstractJsonExtractFunction(
      String name,
      BasicTypeReference<?> type,
      String cast,
      String transformFunction,
      Collector<CharSequence, ?, String> collectPath) {
    super(name, type);
    this.cast = cast;
    this.transformFunction = transformFunction;
    this.collectPath = collectPath;
  }

  @Override
  public void render(
      SqlAppender sqlAppender,
      List<? extends SqlAstNode> sqlAstArguments,
      ReturnableType<?> returnType,
      SqlAstTranslator<?> translator) {

    if (sqlAstArguments.size() < 2) {
      throw new PersistenceException(
          "Invalid use of 'json_extract', requires at least 2 arguments.");
    }

    final SqlAstNode field = sqlAstArguments.getFirst();
    final List<? extends SqlAstNode> pathArgs = sqlAstArguments.subList(1, sqlAstArguments.size());

    if (cast != null) {
      sqlAppender.appendSql("CAST(NULLIF(");
    }

    if (transformFunction != null) {
      sqlAppender.appendSql(transformFunction);
      sqlAppender.appendSql("(");
    }

    sqlAppender.appendSql(getName());
    sqlAppender.appendSql("(");
    translator.render(field, SqlAstNodeRenderingMode.DEFAULT);
    sqlAppender.appendSql(", ");
    renderPath(sqlAppender, pathArgs, returnType, translator);
    sqlAppender.appendSql(")");

    if (transformFunction != null) {
      sqlAppender.appendSql(")");
    }

    if (cast != null) {
      sqlAppender.appendSql(", '') AS ");
      sqlAppender.appendSql(cast);
      sqlAppender.appendSql(")");
    }
  }

  /**
   * Renders the JSON path part.
   *
   * <p>Default implementation uses {@link #collectPath}.
   *
   * @param sqlAppender The SQL appender to which the rendered SQL will be added.
   * @param pathArgs A list of SQL AST nodes representing the JSON path components.
   * @param returnType The return type of the rendered function.
   * @param translator The SQL AST translator.
   * @throws NullPointerException if {@link #collectPath} is {@code null}.
   */
  public void renderPath(
      SqlAppender sqlAppender,
      List<? extends SqlAstNode> pathArgs,
      ReturnableType<?> returnType,
      SqlAstTranslator<?> translator) {

    Objects.requireNonNull(collectPath, "Either set collectPath or override renderPath.");

    final String path =
        pathArgs.stream()
            .map(
                arg ->
                    arg instanceof Literal
                        ? ((Literal) arg).getLiteralValue().toString()
                        : arg.toString())
            .map(AbstractJsonExtractFunction::validateArg)
            .collect(collectPath);
    sqlAppender.appendSql(path);
  }

  private static String validateArg(String name) {
    if (ARGS_PATTERN.matcher(name).matches()) {
      return name;
    }
    throw new PersistenceException("Invalid json field: " + name);
  }
}
