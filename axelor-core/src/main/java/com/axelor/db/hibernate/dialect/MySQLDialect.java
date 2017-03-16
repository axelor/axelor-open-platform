/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
import org.hibernate.dialect.MySQL57Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import com.axelor.db.hibernate.type.JsonSqlTypeDescriptor;
import com.axelor.db.hibernate.type.JsonType;

public class MySQLDialect extends MySQL57Dialect {

	static class JsonExtractFunction extends AbstractJsonExtractFunction {

		public JsonExtractFunction(Type type, String cast) {
			super("json_extract", type, cast);
		}

		@Override
		public String transformPath(List<String> path) {
			return path.stream()
					.map(item -> item.substring(1, item.length() - 1))
					.collect(Collectors.joining(".", "'$.", "'"));
		}
		
		@Override
		protected String transformFunction(String func) {
			return String.format("json_unquote(%s)", func);
		}
	}

	public MySQLDialect() {
		super();
		registerColumnType(Types.OTHER, "json");
		registerFunction("json_extract", new JsonExtractFunction(StandardBasicTypes.STRING, null));
		registerFunction("json_extract_text", new JsonExtractFunction(StandardBasicTypes.STRING, null));
		registerFunction("json_extract_boolean", new JsonExtractFunction(StandardBasicTypes.BOOLEAN, null));
		registerFunction("json_extract_integer", new JsonExtractFunction(StandardBasicTypes.INTEGER, "signed"));
		registerFunction("json_extract_decimal", new JsonExtractFunction(StandardBasicTypes.BIG_DECIMAL, "decimal(64,4)"));
	}
	
	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);
		typeContributions.contributeType(new JsonType(JsonSqlTypeDescriptor.INSTANCE));
	}
}
