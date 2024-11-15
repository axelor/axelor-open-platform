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
package com.axelor.db.hibernate.dialect;

import static org.hibernate.type.SqlTypes.OTHER;

import com.axelor.db.hibernate.dialect.function.PostgreSQLJsonExtractFunction;
import com.axelor.db.hibernate.dialect.function.PostgreSQLJsonSetFunction;
import com.axelor.db.internal.DBHelper;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.type.StandardBasicTypes;

public class AxelorPostgreSQLDialect extends PostgreSQLDialect {

  public AxelorPostgreSQLDialect(DialectResolutionInfo info) {
    super(info);
  }

  @Override
  protected String columnType(int sqlTypeCode) {
    if (sqlTypeCode == OTHER) {
      return "jsonb";
    }
    return super.columnType(sqlTypeCode);
  }

  @Override
  public void initializeFunctionRegistry(FunctionContributions functionContributions) {
    super.initializeFunctionRegistry(functionContributions);

    functionContributions
        .getFunctionRegistry()
        .register("json_set", new PostgreSQLJsonSetFunction());
    functionContributions
        .getFunctionRegistry()
        .register(
            "json_extract", new PostgreSQLJsonExtractFunction(StandardBasicTypes.STRING, null));
    functionContributions
        .getFunctionRegistry()
        .register(
            "json_extract_text",
            new PostgreSQLJsonExtractFunction(StandardBasicTypes.STRING, null));
    functionContributions
        .getFunctionRegistry()
        .register(
            "json_extract_boolean",
            new PostgreSQLJsonExtractFunction(StandardBasicTypes.BOOLEAN, "boolean"));
    functionContributions
        .getFunctionRegistry()
        .register(
            "json_extract_integer",
            new PostgreSQLJsonExtractFunction(StandardBasicTypes.INTEGER, "integer"));
    functionContributions
        .getFunctionRegistry()
        .register(
            "json_extract_decimal",
            new PostgreSQLJsonExtractFunction(StandardBasicTypes.BIG_DECIMAL, "numeric"));

    if (DBHelper.isUnaccentEnabled()) {
      functionContributions
          .getFunctionRegistry()
          .registerNamed(
              "unaccent",
              functionContributions
                  .getTypeConfiguration()
                  .getBasicTypeRegistry()
                  .resolve(StandardBasicTypes.STRING));
    }
  }
}
