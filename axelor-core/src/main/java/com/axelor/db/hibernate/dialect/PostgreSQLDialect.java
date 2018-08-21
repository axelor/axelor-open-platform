/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import com.axelor.db.hibernate.type.EncryptedTextType;
import com.axelor.db.hibernate.type.JsonType;
import com.axelor.db.hibernate.type.JsonbSqlTypeDescriptor;
import java.sql.Types;
import java.util.List;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQL94Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

public class PostgreSQLDialect extends PostgreSQL94Dialect {

  static class JsonExtractFunction extends AbstractJsonExtractFunction {

    public JsonExtractFunction(Type type, String cast) {
      super("jsonb_extract_path_text", type, cast);
    }

    @Override
    protected String transformPath(List<String> path) {
      return String.join(", ", path);
    }
  }

  public PostgreSQLDialect() {
    super();
    registerColumnType(Types.OTHER, "jsonb");
    registerFunction("json_extract", new JsonExtractFunction(StandardBasicTypes.STRING, null));
    registerFunction("json_extract_text", new JsonExtractFunction(StandardBasicTypes.STRING, null));
    registerFunction(
        "json_extract_boolean", new JsonExtractFunction(StandardBasicTypes.BOOLEAN, "boolean"));
    registerFunction(
        "json_extract_integer", new JsonExtractFunction(StandardBasicTypes.INTEGER, "integer"));
    registerFunction(
        "json_extract_decimal", new JsonExtractFunction(StandardBasicTypes.BIG_DECIMAL, "numeric"));
  }

  @Override
  public void contributeTypes(
      TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
    super.contributeTypes(typeContributions, serviceRegistry);
    typeContributions.contributeType(new JsonType(JsonbSqlTypeDescriptor.INSTANCE));
    typeContributions.contributeType(EncryptedTextType.INSTANCE);
  }
}
