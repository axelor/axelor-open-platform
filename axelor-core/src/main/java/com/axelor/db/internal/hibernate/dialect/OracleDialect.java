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
package com.axelor.db.internal.hibernate.dialect;

import java.sql.Types;
import java.util.List;

import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

public class OracleDialect extends Oracle12cDialect {

	static class JsonValueFunction extends StandardSQLFunction {

		private String returning;

		public JsonValueFunction(Type registeredType, String returning) {
			super("json_value", registeredType);
			this.returning = returning;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) {
			final StringBuilder buf = new StringBuilder();
			buf.append(getName()).append('(');
			buf.append(arguments.get(0));
			buf.append(", '$.");
			for (int i = 1; i < arguments.size(); i++) {
				final String argument = (String) arguments.get(i);
				buf.append(argument.substring(1, argument.length() - 1));
				if (i < arguments.size() - 1) {
					buf.append(".");
				}
			}
			buf.append("'");
			if (returning != null) {
				buf.append(" returning ").append(returning);
			}
			buf.append(')');
			return buf.toString();
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
}
