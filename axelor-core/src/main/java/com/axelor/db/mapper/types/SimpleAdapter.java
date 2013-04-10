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

		if (!type.isPrimitive() || isNullable(annotations))
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
	
	private boolean isNullable(Annotation[] annotations){
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
