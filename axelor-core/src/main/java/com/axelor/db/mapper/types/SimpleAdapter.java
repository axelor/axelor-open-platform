/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.db.mapper.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import javax.persistence.Column;

import com.axelor.db.mapper.TypeAdapter;

public class SimpleAdapter implements TypeAdapter<Object> {

	@Override
	public Object adapt(Object value, Class<?> type, Type genericType,
			Annotation[] annotations) {

		if (value == null || (value instanceof String && "".equals(((String) value).trim()))) {
			return adaptNull(value, type, genericType, annotations);
		}
		
		// try a constructor with exact value type
		try {
			return type.getConstructor(new Class<?>[] { value.getClass() })
					.newInstance(new Object[] { value });
		} catch (Exception e) {
		}

		if (type == String.class) {
			if (value == null || value instanceof String) {
				return value;
			}
			return value.toString();
		}
		
		if (type == byte[].class && value instanceof String) {
			return ((String) value).getBytes();
		}

		if (type == Boolean.TYPE || type == Boolean.class)
			return Boolean.valueOf(value.toString());

		if (type == Character.TYPE || type == Character.class)
			return Character.valueOf(value.toString().charAt(0));

		if (type == Byte.TYPE || type == Byte.class)
			return Byte.valueOf(value.toString());
	
		if (type == Short.TYPE || type == Short.class)
			return Short.valueOf(value.toString());

		if (type == Integer.TYPE || type == Integer.class)
			return Integer.valueOf(value.toString());

		if (type == Long.TYPE || type == Long.class)
			return Long.valueOf(value.toString());

		if (type == Float.TYPE || type == Float.class)
			return Float.valueOf(value.toString());

		if (type == Date.class)
			return Date.valueOf(value.toString());

		if (type == Time.class)
			return Time.valueOf(value.toString());

		if (type == Timestamp.class)
			return Timestamp.valueOf(value.toString());
		
		return value;
	}

	public Object adaptNull(Object value, Class<?> type, Type genericType,
			Annotation[] annotations) {

		if (isNullable(type, annotations))
			return null;

		if (type == boolean.class)
			return false;

		if (type == int.class)
			return 0;

		if (type == long.class)
			return 0L;

		if (type == double.class)
			return 0.0;

		if (type == short.class)
			return 0.0F;

		if (type == char.class)
			return ' ';

		return null;
	}
	
	private boolean isNullable(Class<?> type, Annotation[] annotations){
		if (type.isPrimitive()) {
			return false;
		}
		if(annotations == null || annotations.length == 0){
			return true;
		}
		
		for (Annotation annotation : annotations) {
			if(annotation instanceof Column) {
				return ((Column) annotation).nullable();
			}
		}
		
		return false;
	}
}
