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
package com.axelor.rpc;

import java.util.Map;
import java.util.Objects;

import javax.script.SimpleBindings;

import com.axelor.common.StringUtils;
import com.axelor.db.mapper.Mapper;

/**
 * The Context class represents an {@link ActionRequest} context.
 * 
 * <p>
 * The request context is mapped to a proxy instance of the bean class on which
 * the action is being performed. The proxy instance can be accessed via
 * {@link #asType(Class)} method.
 * 
 * <p>
 * Example (Java):
 *
 * <pre>
 * Context context = request.getContext();
 * SaleOrderLine soLine = context.asType(SaleOrderLine.class);
 * SaleOrder so = context.getParentContext().asType(SaleOrder.class);
 * </pre>
 *
 * Example (Groovy):
 *
 * <pre>
 * def context = request.context
 * def soLine = context as SaleOrderLine
 * def so = context.parentContext as SaleOrder
 * </pre>
 *
 * The instance returned from the context is a detached proxy object and should
 * not be used with JPA/Hibernate session. It's only for convenience to get the
 * context values using the bean methods.
 *
 */
public class Context extends SimpleBindings {

	private static final String KEY_MODEL = "_model";
	private static final String KEY_PARENT = "_parent";
	private static final String KEY_PARENT_CONTEXT = "parentContext";

	private final Map<String, Object> values;

	private final Mapper mapper;

	private final Class<?> beanClass;

	private Object proxy;
	
	private Context parent;

	/**
	 * Create a new {@link Context} for the given bean class using the given
	 * context values.
	 * 
	 * @param values
	 *            the context values
	 * @param beanClass
	 *            the context bean class
	 */
	public Context(Map<String, Object> values, Class<?> beanClass) {
		super(values);
		this.values = Objects.requireNonNull(values);
		this.beanClass = Objects.requireNonNull(beanClass);
		this.mapper = Mapper.of(beanClass);
	}

	private Object getProxy() {
		if (proxy == null) {
			proxy = ContextProxy.create(values, beanClass);
		}
		return proxy;
	}

	/**
	 * Get parent context.
	 * 
	 * @return the parent context if exist
	 */
	@SuppressWarnings("unchecked")
	public Context getParent() {
		if (parent != null) {
			return parent;
		}
		final Object value = values.get(KEY_PARENT);
		if (value == null) {
			return null;
		}
		try {
			Map<String, Object> valueMap = (Map<String, Object>) value;
			Class<?> parentClass = Class.forName((String) valueMap.get(KEY_MODEL));
			parent = new Context(valueMap, parentClass);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return parent;
	}

	/**
	 * @see #getParent()
	 */
	@Deprecated
	public Context getParentContext() {
		return getParent();
	}

	public Class<?> getContextClass() {
		return beanClass;
	}

	@SuppressWarnings("unchecked")
	public <T> T asType(Class<T> type) {
		final T bean = (T) getProxy();
		if (!type.isInstance(bean)) {
			throw new IllegalArgumentException(
					String.format("Invalid type {}, should be {}", type.getName(), beanClass.getName()));
		}
		return bean;
	}

	private void checkKey(Object key) {
		if (key == null) {
			throw new NullPointerException("key can not be null");
		}
		if (!(key instanceof String)) {
			throw new ClassCastException("key should be a String");
		}
		if (StringUtils.isEmpty((String) key)) {
			throw new IllegalArgumentException("key can not be empty");
		}
	}

	@Override
	public boolean containsKey(Object key) {
		checkKey(key);
		if (super.containsKey(key) || KEY_PARENT_CONTEXT.equals(key)) {
			return true;
		}
		return mapper.getProperty((String) key) != null;
	}

	@Override
	public Object get(Object key) {
		checkKey(key);
		if (KEY_PARENT_CONTEXT.equals(key)) {
			return getParent();
		}
		if (mapper.getProperty((String) key) != null) {
			return mapper.get(getProxy(), (String) key);
		}
		return super.get(key);
	}
	
	@Override
	public Object put(String name, Object value) {
		if (mapper.getProperty(name) != null) {
			return mapper.set(getProxy(), name, value);
		}
		return super.put(name, value);
	}
}
