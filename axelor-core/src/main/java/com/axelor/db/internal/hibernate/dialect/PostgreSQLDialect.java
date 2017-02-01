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

import org.hibernate.dialect.PostgreSQL94Dialect;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.type.StandardBasicTypes;

public class PostgreSQLDialect extends PostgreSQL94Dialect {

	public PostgreSQLDialect() {
		super();
		registerColumnType(Types.OTHER, "jsonb");
		registerFunction("json_extract",
				new VarArgsSQLFunction(StandardBasicTypes.STRING, "jsonb_extract_path_text(", ",", ")"));
		registerFunction("json_extract_text",
				new VarArgsSQLFunction(StandardBasicTypes.STRING, "jsonb_extract_path_text(", ",", ")"));
		registerFunction("json_extract_boolean",
				new VarArgsSQLFunction(StandardBasicTypes.BOOLEAN, "jsonb_extract_path_text(", ",", ")::boolean"));
		registerFunction("json_extract_integer",
				new VarArgsSQLFunction(StandardBasicTypes.INTEGER, "jsonb_extract_path_text(", ",", ")::integer"));
		registerFunction("json_extract_decimal",
				new VarArgsSQLFunction(StandardBasicTypes.BIG_DECIMAL, "jsonb_extract_path_text(", ",", ")::numeric"));
	}
}
