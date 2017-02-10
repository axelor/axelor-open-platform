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
package com.axelor.db.mapper;

public enum PropertyType {
	STRING,
	TEXT,
	BOOLEAN,
	INTEGER,
	LONG,
	DOUBLE,
	DECIMAL,
	DATE,
	TIME,
	DATETIME,
	BINARY,
	ONE_TO_ONE,
	MANY_TO_ONE,
	ONE_TO_MANY,
	MANY_TO_MANY;
	
	public static PropertyType get(String value) {
		assert value != null;
		try {
			return PropertyType.valueOf(value);
		} catch(Exception e){
			if (value.equals("INT")) return PropertyType.INTEGER;
			if (value.equals("FLOAT")) return PropertyType.DOUBLE;
			if (value.equals("BIGDECIMAL")) return PropertyType.DECIMAL;
			if (value.equals("LOCALDATE")) return PropertyType.DATE;
			if (value.equals("LOCALTIME")) return PropertyType.TIME;
			if (value.equals("LOCALDATETIME")) return PropertyType.DATETIME;
			if (value.equals("CALENDAR")) return PropertyType.DATETIME;
			if (value.equals("BYTE[]")) return PropertyType.BINARY;
		}
		return null;
	}
}