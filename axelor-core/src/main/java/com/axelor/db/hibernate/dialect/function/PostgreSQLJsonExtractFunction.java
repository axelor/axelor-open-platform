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
