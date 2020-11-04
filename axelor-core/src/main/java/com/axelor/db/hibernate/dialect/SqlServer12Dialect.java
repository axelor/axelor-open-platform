/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2020 Axelor (<http://axelor.com>).
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

import com.axelor.db.hibernate.dialect.unique.SqlServer2008UniqueDelegate;
import com.axelor.db.hibernate.type.EncryptedTextType;
import com.axelor.db.hibernate.type.JsonTextSqlTypeDescriptor;
import com.axelor.db.hibernate.type.JsonType;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.dialect.unique.InformixUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import java.util.List;
import java.util.stream.Collectors;

public class SqlServer12Dialect extends SQLServer2012Dialect {

  private final UniqueDelegate uniqueDelegate;

  static class JsonValueFunction extends AbstractJsonExtractFunction {

    public JsonValueFunction(Type type, String cast) {
      super("JSON_VALUE", type, cast);
    }

    @Override
    protected String transformPath(List<String> path) {
      return path.stream()
          .map(item -> item.substring(1, item.length() - 1))
          .collect(Collectors.joining(".", "'$.", "'"));
    }
  }

  @Override
  public UniqueDelegate getUniqueDelegate() {
    return uniqueDelegate;
  }

  public SqlServer12Dialect() {
    super();

    registerFunction("json_extract", new JsonValueFunction(StandardBasicTypes.STRING, null));
    registerFunction("json_extract_text", new JsonValueFunction(StandardBasicTypes.STRING, null));
    registerFunction(
        "json_extract_boolean", new JsonValueFunction(StandardBasicTypes.BOOLEAN, "bit"));
    registerFunction(
        "json_extract_integer", new JsonValueFunction(StandardBasicTypes.INTEGER, "bigint"));
    registerFunction(
        "json_extract_decimal", new JsonValueFunction(StandardBasicTypes.BIG_DECIMAL, "numeric(28,12)"));
    uniqueDelegate = new SqlServer2008UniqueDelegate( this );
  }

  @Override
  public void contributeTypes(
      TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
    super.contributeTypes(typeContributions, serviceRegistry);
    typeContributions.contributeType(new JsonType(JsonTextSqlTypeDescriptor.INSTANCE));
    typeContributions.contributeType(EncryptedTextType.INSTANCE);
  }
}
