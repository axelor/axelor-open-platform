package com.axelor.db.mapper.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.TypeAdapter;

public class MapAdapter implements TypeAdapter<Map<?, ?>> {

	@SuppressWarnings("unchecked")
	@Override
	public Model adapt(Object value, Class<?> type, Type genericType, Annotation[] annotations) {
		if (value instanceof Model)
			return (Model) value;
		return Mapper.toBean((Class<Model>) type, (Map<String, Object>) value);
	}
}
