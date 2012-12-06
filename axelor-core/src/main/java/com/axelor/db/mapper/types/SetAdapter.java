package com.axelor.db.mapper.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.TypeAdapter;

public class SetAdapter implements TypeAdapter<Set<?>> {

	@SuppressWarnings("unchecked")
	@Override
	public Set<Model> adapt(Object value, Class<?> type, Type genericType, Annotation[] annotations) {
		
		Class<?> fieldType = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];

		Set<Model> val = new HashSet<Model>();
		for (Object obj : (Collection<?>) value) {
			if (obj instanceof Map)
				val.add(Mapper.toBean((Class<Model>) fieldType, (Map<String, Object>) obj));
			if (obj instanceof Model)
				val.add((Model) obj);
		}
		return val;
	}
}
