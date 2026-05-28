/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.dialect.function;

import java.util.List;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicTypeReference;

public class PostgreSQLJsonExtractFunction extends AbstractJsonExtractFunction {

  public PostgreSQLJsonExtractFunction(BasicTypeReference<?> type, String cast) {
    super("jsonb_extract_path_text", type, cast);
  }

  @Override
  public void renderPath(
      SqlAppender sqlAppender,
      List<? extends SqlAstNode> pathArgs,
      ReturnableType<?> returnType,
      SqlAstTranslator<?> translator) {

    translator.render(pathArgs.getFirst(), SqlAstNodeRenderingMode.DEFAULT);

    for (final SqlAstNode pathArg : pathArgs.subList(1, pathArgs.size())) {
      sqlAppender.appendSql(", ");
      translator.render(pathArg, SqlAstNodeRenderingMode.DEFAULT);
    }
  }
}
