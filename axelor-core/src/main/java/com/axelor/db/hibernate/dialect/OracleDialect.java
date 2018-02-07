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

import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import com.axelor.db.hibernate.type.JsonTextSqlTypeDescriptor;
import com.axelor.db.hibernate.type.JsonType;

public class OracleDialect extends Oracle12cDialect {

	static class JsonValueFunction extends AbstractJsonExtractFunction {

		public JsonValueFunction(Type type, String cast) {
			super("json_value", type, cast);
		}

		@Override
		protected String transformPath(List<String> path) {
			return path.stream()
					.map(item -> item.substring(1, item.length() - 1))
					.collect(Collectors.joining(".", "'$.", "'"));
		}
	}

	public OracleDialect() {
		super();
		registerColumnType(Types.LONGVARCHAR, "clob");
		registerFunction("json_extract", new JsonValueFunction(StandardBasicTypes.STRING, null));
		registerFunction("json_extract_text", new JsonValueFunction(StandardBasicTypes.STRING, null));
		registerFunction("json_extract_boolean", new JsonValueFunction(StandardBasicTypes.BOOLEAN, "number"));
		registerFunction("json_extract_integer", new JsonValueFunction(StandardBasicTypes.INTEGER, "number"));
		registerFunction("json_extract_decimal", new JsonValueFunction(StandardBasicTypes.BIG_DECIMAL, "number"));
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);
		typeContributions.contributeType(new JsonType(JsonTextSqlTypeDescriptor.INSTANCE));
	}
}
