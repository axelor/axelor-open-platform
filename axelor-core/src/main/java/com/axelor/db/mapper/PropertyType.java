/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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