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

import com.axelor.db.hibernate.dialect.function.MySQLJsonExtractFunction;
import com.axelor.db.hibernate.dialect.function.MySQLJsonSetFunction;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.type.StandardBasicTypes;

public class AxelorMySQLDialect extends MySQLDialect {

  public AxelorMySQLDialect(DatabaseVersion info) {
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
