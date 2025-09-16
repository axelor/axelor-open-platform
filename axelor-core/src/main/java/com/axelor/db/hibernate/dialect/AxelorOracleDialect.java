/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.dialect;

import static org.hibernate.type.SqlTypes.LONGVARCHAR;

import com.axelor.db.hibernate.dialect.function.OracleJsonExtractFunction;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.type.StandardBasicTypes;

public class AxelorOracleDialect extends OracleDialect {

  public AxelorOracleDialect(DialectResolutionInfo info) {
    super(info);
  }

  @Override
  protected String columnType(int sqlTypeCode) {
    if (sqlTypeCode == LONGVARCHAR) {
      return "clob";
    }
    return super.columnType(sqlTypeCode);
  }

  @Override
  public void initializeFunctionRegistry(FunctionContributions functionContributions) {
    super.initializeFunctionRegistry(functionContributions);

    functionContributions
        .getFunctionRegistry()
        .register("json_extract", new OracleJsonExtractFunction(StandardBasicTypes.STRING, null));
    functionContributions
        .getFunctionRegistry()
        .register(
            "json_extract_text", new OracleJsonExtractFunction(StandardBasicTypes.STRING, null));
    functionContributions
        .getFunctionRegistry()
        .register(
            "json_extract_boolean",
            new OracleJsonExtractFunction(StandardBasicTypes.BOOLEAN, "number"));
    functionContributions
        .getFunctionRegistry()
        .register(
            "json_extract_integer",
            new OracleJsonExtractFunction(StandardBasicTypes.INTEGER, "number"));
    functionContributions
        .getFunctionRegistry()
        .register(
            "json_extract_decimal",
            new OracleJsonExtractFunction(StandardBasicTypes.BIG_DECIMAL, "number"));
  }
}
