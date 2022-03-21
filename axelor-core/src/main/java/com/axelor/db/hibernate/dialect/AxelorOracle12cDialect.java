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

import com.axelor.db.hibernate.dialect.function.OracleJsonExtractFunction;
import com.axelor.db.hibernate.type.EncryptedTextType;
import com.axelor.db.hibernate.type.JsonTextSqlTypeDescriptor;
import com.axelor.db.hibernate.type.JsonType;
import java.sql.Types;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;

public class AxelorOracle12cDialect extends Oracle12cDialect {

  public AxelorOracle12cDialect() {
    super();
    registerColumnType(Types.LONGVARCHAR, "clob");
    registerFunction(
        "json_extract", new OracleJsonExtractFunction(StandardBasicTypes.STRING, null));
    registerFunction(
        "json_extract_text", new OracleJsonExtractFunction(StandardBasicTypes.STRING, null));
    registerFunction(
        "json_extract_boolean",
        new OracleJsonExtractFunction(StandardBasicTypes.BOOLEAN, "number"));
    registerFunction(
        "json_extract_integer",
        new OracleJsonExtractFunction(StandardBasicTypes.INTEGER, "number"));
    registerFunction(
        "json_extract_decimal",
        new OracleJsonExtractFunction(StandardBasicTypes.BIG_DECIMAL, "number"));
  }

  @Override
  public void contributeTypes(
      TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
    super.contributeTypes(typeContributions, serviceRegistry);
    typeContributions.contributeType(new JsonType(JsonTextSqlTypeDescriptor.INSTANCE));
    typeContributions.contributeType(EncryptedTextType.INSTANCE);
  }
}
