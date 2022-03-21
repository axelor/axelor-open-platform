/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.db.hibernate.dialect;

import com.axelor.db.hibernate.dialect.function.PostgreSQLJsonExtractFunction;
import com.axelor.db.hibernate.dialect.function.PostgreSQLJsonSetFunction;
import com.axelor.db.hibernate.type.EncryptedTextType;
import com.axelor.db.hibernate.type.JsonType;
import com.axelor.db.hibernate.type.JsonbSqlTypeDescriptor;
import java.sql.Types;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;

public class AxelorPostgreSQL95Dialect extends PostgreSQL95Dialect {

  public AxelorPostgreSQL95Dialect() {
    super();
    registerColumnType(Types.OTHER, "jsonb");
    registerFunction("json_set", new PostgreSQLJsonSetFunction());
    registerFunction(
        "json_extract", new PostgreSQLJsonExtractFunction(StandardBasicTypes.STRING, null));
    registerFunction(
        "json_extract_text", new PostgreSQLJsonExtractFunction(StandardBasicTypes.STRING, null));
    registerFunction(
        "json_extract_boolean",
        new PostgreSQLJsonExtractFunction(StandardBasicTypes.BOOLEAN, "boolean"));
    registerFunction(
        "json_extract_integer",
        new PostgreSQLJsonExtractFunction(StandardBasicTypes.INTEGER, "integer"));
    registerFunction(
        "json_extract_decimal",
        new PostgreSQLJsonExtractFunction(StandardBasicTypes.BIG_DECIMAL, "numeric"));
  }

  @Override
  public void contributeTypes(
      TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
    super.contributeTypes(typeContributions, serviceRegistry);
    typeContributions.contributeType(new JsonType(JsonbSqlTypeDescriptor.INSTANCE));
    typeContributions.contributeType(EncryptedTextType.INSTANCE);
  }
}
