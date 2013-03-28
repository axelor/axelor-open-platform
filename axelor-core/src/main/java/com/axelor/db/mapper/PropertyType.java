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