/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
