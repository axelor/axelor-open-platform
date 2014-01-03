/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.db.mapper;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.axelor.db.annotations.NameColumn;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * This class can be used to map params to Java bean using reflection. It also
 * provides convenient methods to get/set values to a bean instance.
 *
 */
public class Mapper {

	private static final LoadingCache<Class<?>, Mapper> MAPPER_CACHE = CacheBuilder
			.newBuilder()
			.maximumSize(1000)
			.weakKeys()
			.build(new CacheLoader<Class<?>, Mapper>() {

				@Override
				public Mapper load(Class<?> key) throws Exception {
					return new Mapper(key);
				}
			});

	private static final Object[] NULL_ARGUMENTS = {};

	private Map<String, Method> getters = new HashMap<String, Method>();
	private Map<String, Method> setters = new HashMap<String, Method>();

	private Map<String, Class<?>> types = new HashMap<String, Class<?>>();
	private Map<String, Property> fields = new HashMap<String, Property>();

	private Property nameField;

	private Class<?> beanClass;

	private Mapper(Class<?> beanClass) {
		Preconditions.checkNotNull(beanClass);
		this.beanClass = beanClass;
		try {
			BeanInfo info = Introspector.getBeanInfo(beanClass, Object.class);
			for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {

				String name = descriptor.getName();
				Method getter = descriptor.getReadMethod();
				Method setter = descriptor.getWriteMethod();
				Class<?> type = descriptor.getPropertyType();

				if (getter != null) {
					getters.put(name, getter);
					try {
						fields.put(name, new Property(
								beanClass, name, type,
								getter.getGenericReturnType(),
								getAnnotations(name, getter)));
					}catch(Exception e){
						continue;
					}
				}
				if (setter != null) {
					setters.put(name, setter);
				}
				types.put(name, type);
			}
		} catch (IntrospectionException e) {
		}
	}

	private static final Cache<Method, Annotation[]> ANNOTATION_CACHE = CacheBuilder
			.newBuilder()
			.maximumSize(1000)
			.weakKeys()
			.build();

	private Annotation[] getAnnotations(String name, Method method) {
		Annotation[] found = ANNOTATION_CACHE.getIfPresent(method);
		if (found != null) {
			return found;
		}

		final List<Annotation> all = Lists.newArrayList();
		try {
			final Field field = getField(beanClass, name);
			for (Annotation a : field.getAnnotations()) {
				all.add(a);
			}
		} catch (Exception e) {
		}

		for (Annotation a : method.getAnnotations()) {
			all.add(a);
		}

		found = all.toArray(new Annotation[] {});
		ANNOTATION_CACHE.put(method, found);

		return found;
	}

	private Field getField(Class<?> klass, String name) {
		try {
			return klass.getDeclaredField(name);
		} catch (NoSuchFieldException e) {
			return getField(klass.getSuperclass(), name);
		}
	}

	/**
	 * Create a {@link Mapper} for the given Java Bean class by introspecting
	 * all it's properties.
	 * <p>
	 * If the {@link Mapper} class has been previously created for the given
	 * class, then the {@link Mapper} class is retrieved from the cache.
	 *
	 * @param klassJava
	 *            bean class
	 */
	public static Mapper of(Class<?> klass) {
		try {
			return MAPPER_CACHE.get(klass);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get all the properties.
	 *
	 * @return an array of {@link Property}
	 */
	public Property[] getProperties() {
		return fields.values().toArray(new Property[] {});
	}

	/**
	 * Get the {@link Property} of the given name.
	 *
	 * @param name
	 *            name of the property
	 * @return a Property or null if property doesn't exist.
	 */
	public Property getProperty(String name) {
		return fields.get(name);
	}

	/**
	 * Get the property of the name field.
	 *
	 * A name field annotated with {@link NameColumn} or a field with
	 * name <code>name</code> is considered name field.
	 *
	 * @return a property
	 */
	public Property getNameField() {
		if (nameField != null) {
			return nameField;
		}
		for(Property property : fields.values()) {
			if (property.isNameColumn()) {
				return nameField = property;
			}
		}
		return nameField = getProperty("name");
	}

	/**
	 * Get the bean class this mapper operates on.
	 *
	 * @return the bean class
	 */
	public Class<?> getBeanClass() {
		return beanClass;
	}

	/**
	 * Get the getter method of the given property.
	 *
	 * @param name
	 *            name of the property
	 * @return getter method or null if property is write-only
	 */
	public Method getGetter(String name) {
		return getters.get(name);
	}

	/**
	 * Get the setter method of the given property.
	 *
	 * @param name
	 *            name of the property
	 * @return setter method or null if property is read-only
	 */
	public Method getSetter(String name) {
		return setters.get(name);
	}

	/**
	 * Get the value of given property from the given bean. It returns
	 * <code>null</code> if property doesn't exist.
	 *
	 * @param bean
	 *            the bean
	 * @param name
	 *            name of the property
	 * @return property value
	 */
	public Object get(Object bean, String name) {

		Preconditions.checkNotNull(bean);
		Preconditions.checkNotNull(name);

		Preconditions.checkArgument(beanClass.isInstance(bean));
		Preconditions.checkArgument(!name.trim().equals(""));

		Method method = getters.get((String) name);
		try {
			return method.invoke(bean, NULL_ARGUMENTS);
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Set the property of the given bean with the provided value.
	 *
	 * @param bean
	 *            the bean
	 * @param name
	 *            name of the property
	 * @param value
	 *            value for the property
	 * @return old value of the property
	 */
	public Object set(Object bean, String name, Object value) {

		Preconditions.checkNotNull(bean);
		Preconditions.checkNotNull(name);

		Preconditions.checkArgument(beanClass.isInstance(bean));
		Preconditions.checkArgument(!name.trim().equals(""));

		Object oldValue = get(bean, name);
		Method method = setters.get(name);
		if (method == null) {
			throw new IllegalArgumentException("The bean of type: "
					+ beanClass.getName() + " has no property called: " + name);
		}

		Class<?> actualType = method.getParameterTypes()[0];
		Type genericType = method.getGenericParameterTypes()[0];
		Annotation[] annotations = getAnnotations(name, method);

		value = Adapter.adapt(value, actualType, genericType, annotations);

		try {
			method.invoke(bean, new Object[] { value });
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		return oldValue;
	}

	/**
	 * Create an object of the given class mapping the given value map to it's
	 * properties.
	 *
	 * @param klass
	 *            class of the bean
	 * @param values
	 *            value map
	 * @return an instance of the given class
	 */
	public static <T> T toBean(Class<T> klass, Map<String, Object> values) {
		T bean = null;
		try {
			bean = klass.newInstance();
		} catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}
		if (values == null || values.isEmpty()) {
			return bean;
		}
		Mapper mapper = Mapper.of(klass);
		for (String name : values.keySet()) {
			if (mapper.setters.containsKey(name))
				mapper.set(bean, name, values.get(name));
		}

		return bean;
	}

	/**
	 * Create a map from the given bean instance with property names are keys
	 * and their respective values are map values.
	 *
	 * @param bean
	 *            a bean instance
	 * @return a map
	 */
	public static Map<String, Object> toMap(Object bean) {
		if (bean == null) return null;

		Map<String, Object> map = Maps.newHashMap();
		Mapper mapper = Mapper.of(bean.getClass());

		for(Property p : mapper.getProperties()) {
			map.put(p.getName(), p.get(bean));
		}

		return map;
	}
}
