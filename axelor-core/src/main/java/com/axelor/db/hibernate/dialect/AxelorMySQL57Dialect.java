/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

import com.axelor.db.hibernate.dialect.function.MySQLJsonExtractFunction;
import com.axelor.db.hibernate.dialect.function.MySQLJsonSetFunction;
import com.axelor.db.hibernate.type.EncryptedTextType;
import com.axelor.db.hibernate.type.JsonSqlTypeDescriptor;
import com.axelor.db.hibernate.type.JsonType;
import java.sql.Types;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.MySQL57Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;

public class AxelorMySQL57Dialect extends MySQL57Dialect {

  public AxelorMySQL57Dialect() {
    super();
    registerColumnType(Types.OTHER, "json");
    registerFunction("json_set", new MySQLJsonSetFunction());
    registerFunction("json_extract", new MySQLJsonExtractFunction(StandardBasicTypes.STRING, null));
    registerFunction(
        "json_extract_text", new MySQLJsonExtractFunction(StandardBasicTypes.STRING, null));
    registerFunction(
        "json_extract_boolean", new MySQLJsonExtractFunction(StandardBasicTypes.BOOLEAN, null));
    registerFunction(
        "json_extract_integer", new MySQLJsonExtractFunction(StandardBasicTypes.INTEGER, "signed"));
    registerFunction(
        "json_extract_decimal",
        new MySQLJsonExtractFunction(StandardBasicTypes.BIG_DECIMAL, "decimal(64,4)"));
  }

  @Override
  public void contributeTypes(
      TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
    super.contributeTypes(typeContributions, serviceRegistry);
    typeContributions.contributeType(new JsonType(JsonSqlTypeDescriptor.INSTANCE));
    typeContributions.contributeType(EncryptedTextType.INSTANCE);
  }
}
