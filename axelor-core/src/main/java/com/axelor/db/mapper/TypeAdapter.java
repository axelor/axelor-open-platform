package com.axelor.db.mapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public interface TypeAdapter<T> {

	Object adapt(Object value, Class<?> actualType, Type genericType, Annotation[] annotations);
}