/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.dialect;

import static org.hibernate.type.SqlTypes.OTHER;

import com.axelor.db.hibernate.dialect.function.MySQLJsonExtractFunction;
import com.axelor.db.hibernate.dialect.function.MySQLJsonSetFunction;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.type.StandardBasicTypes;

public class AxelorMySQLDialect extends MySQLDialect {

  public AxelorMySQLDialect(DialectResolutionInfo info) {
    super(info);
  }

  @Override
  protected String columnType(int sqlTypeCode) {
    if (sqlTypeCode == OTHER) {
      return "json";
    }
    return super.columnType(sqlTypeCode);
  }

  @Override
  public void initializeFunctionRegistry(FunctionContributions functionContributions) {
    super.initializeFunctionRegistry(functionContributions);

    functionContributions.getFunctionRegistry().register("json_set", new MySQLJsonSetFunction());
    functionContributions
        .getFunctionRegistry()
        .register("json_extract", new MySQLJsonExtractFunction(StandardBasicTypes.STRING, null));
    functionContributions
        .getFunctionRegistry()
        .register(
            "json_extract_text", new MySQLJsonExtractFunction(StandardBasicTypes.STRING, null));
    functionContributions
        .getFunctionRegistry()
        .register(
            "json_extract_boolean", new MySQLJsonExtractFunction(StandardBasicTypes.BOOLEAN, null));
    functionContributions
        .getFunctionRegistry()
        .register(
            "json_extract_integer",
            new MySQLJsonExtractFunction(StandardBasicTypes.INTEGER, "signed"));
    functionContributions
        .getFunctionRegistry()
        .register(
            "json_extract_decimal",
            new MySQLJsonExtractFunction(StandardBasicTypes.BIG_DECIMAL, "decimal(64,4)"));
  }
}
