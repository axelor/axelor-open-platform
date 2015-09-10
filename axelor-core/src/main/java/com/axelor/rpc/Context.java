/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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

import static com.axelor.common.StringUtils.isBlank;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axelor.auth.db.AuditableModel;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.internal.EntityHelper;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.axelor.internal.cglib.proxy.Enhancer;
import com.axelor.internal.cglib.proxy.InvocationHandler;
import com.axelor.script.ScriptBindings;
import com.google.common.base.Preconditions;

/**
 * The Context class represents an {@link ActionRequest} context.<br>
 * <br>
 *
 * The request context is mapped to an instance of the bean class on which the
 * action is being performed. The instance can be accessed via
 * {@link #asType(Class)} method. <br>
 *
 * Example (Java):<br>
 *
 * <pre>
 * Context context = request.getContext();
 * SaleOrderLine soLine = context.asType(SaleOrderLine.class);
 * SaleOrder so = context.getParentContext().asType(SaleOrder.class);
 * </pre>
 *
 * Example (Groovy):<br>
 *
 * <pre>
 * def context = request.context
 * def soLine = context as SaleOrderLine
 * def so = context.parentContext as SaleOrder
 * </pre>
 *
 * The bean instanced returned from the context is a detached object (possibly a
 * cglib proxy) and should not be used with JPA/Hibernate session. It's only for
 * convenience to get the context values using the bean methods.
 *
 */
public class Context extends HashMap<String, Object> {

	private static final long serialVersionUID = -5405070533303843069L;

	private static final String FIELD_ID = "id";
	private static final String FIELD_VERSION = "version";
	private static final String FIELD_SELECTED = "selected";

	private static final String KEY_MODEL = "_model";
	private static final String KEY_PARENT = "_parent";
	private static final String KEY_PARENT_CONTEXT = "parentContext";
	private static final String KEY_FORM = "_form";

	private Class<?> beanClass;
	private Object beanInstance;

	private Context(Map<String, Object> data, Object bean, Class<?> beanClass) {
		super(data);
		this.beanInstance = bean;
		this.beanClass = beanClass;
	}

	/**
	 * Create a proxy of the given bean instance if it's backed by database
	 * record.
	 *
	 */
	private static Object createProxy(final Object instance, final Map<String, Object> values) {

		if (!(instance instanceof Model) || values == null) {
			return instance;
		}

		final Model bean = (Model) instance;
		if (bean.getId() == null || Enhancer.isEnhanced(bean.getClass()) || JPA.em().contains(bean)) {
			return bean;
		}

		final Class<?> beanClass = EntityHelper.getEntityClass(bean);
		final Enhancer enhancer = new Enhancer();

		enhancer.setSuperclass(beanClass);
		enhancer.setCallback(new InvocationHandler() {

			private Object managed;

			private Map<String, String> getters;
			private Set<String> computed;

			private void init() {

				if (getters != null) {
					return;
				}

				getters = new HashMap<>();
				computed = new HashSet<>();

				Mapper mapper = Mapper.of(beanClass);
				for (Property property : mapper.getProperties()) {
					String name = property.getName();
					if (mapper.getGetter(name) != null) {
						getters.put(mapper.getGetter(name).getName(), name);
					}
					if (property.isVirtual()) {
						computed.add(name);
					}
				}
			}

			private Object managed() {
				if (managed == null) {
					managed = JPA.em().find(beanClass, bean.getId());
				}
				return managed;
			}

			@Override
			public Object invoke(Object obj, Method method, Object[] args) throws Throwable {

				// initialize on-demand
				this.init();

				final String fieldName = getters.get(method.getName());

				// if not a getter, invoke on context bean instance
				if (fieldName == null) {
					return method.invoke(bean, args);
				}
				// if context variable, read from context bean instance
				if (values.containsKey(fieldName) || computed.contains(fieldName)) {
					return method.invoke(bean, args);
				}

				final Object managed = this.managed();
				if (managed == null) {
					return null;
				}

				// read from managed instance
				return method.invoke(managed, args);
			}
		});

		return enhancer.create();
	}

	@SuppressWarnings("all")
	private static Object createOrFind(Property p, Object value, boolean nested) {

		Object bean = value;

		if (value instanceof Map) {
			Map map = (Map) value;
			Object id = map.get(FIELD_ID);
			// if new/updated then create map
			if (map.containsKey(FIELD_VERSION) || id == null) {
				Context ctx =  create(map, p.getTarget(), nested);
				bean = ctx.beanInstance;
			} else {
				bean = JPA.find((Class) p.getTarget(), Long.parseLong(id.toString()));
			}
			if (bean != null && map.containsKey(FIELD_SELECTED))
				Mapper.of(p.getTarget()).set(bean, FIELD_SELECTED, map.get(FIELD_SELECTED));
		}
		if (bean instanceof Model) {
			return bean;
		}
		return null;
	}

	@SuppressWarnings("all")
	public static Context create(Map<String, Object> data, Class<?> beanClass) {
		Preconditions.checkNotNull(beanClass);
		if (data == null) {
			data = new HashMap<>();
		}
		if (ScriptBindings.class.isAssignableFrom(beanClass)) {
			return new Context(data, new ScriptBindings(data), ScriptBindings.class);
		}
		return create(data, beanClass, data.containsKey(KEY_FORM));
	}

	@SuppressWarnings("all")
	private static Context create(Map<String, Object> data, Class<?> beanClass, boolean nested) {

		Preconditions.checkNotNull(beanClass);
		if (data == null) {
			data = new HashMap<>();
		}

		Mapper mapper = Mapper.of(beanClass);
		Object bean = Mapper.toBean(beanClass, null);

		List<String> computed = new ArrayList<>();
		Map<String, Object> validated = new HashMap<>();

		for(String name : data.keySet()) {

			Object value = data.get(name);
			Property p = mapper.getProperty(name);

			if (p == null) {
				if (KEY_PARENT.equals(name)) {
					try {
						Class<?> parentClass = Class.forName((String) ((Map) value).get(KEY_MODEL));
						value = create((Map) value, parentClass, nested);
					} catch (Exception e) {
					}
				}
			}
			else if (p.isVirtual()) {
				computed.add(name);
				continue;
			}
			else if (p.isCollection() && value instanceof Collection) {
				List items = new ArrayList<>();
				for(Object item : (Collection) value) {
					items.add(createOrFind(p, item, nested));
				}
				value = items;
			}
			// non-owning side can't handle the relationship (RM-2616, RM-2457)
			else if (p.getType() == PropertyType.ONE_TO_ONE && !isBlank(p.getMappedBy())) {
				if (nested) continue;
				try {
					value = JPA.em().find(p.getTarget(), Long.parseLong(((Map) value).get(FIELD_ID).toString()));
				} catch (Exception e) {
					continue;
				}
			}
			else if (p.isReference()) {
				value = createOrFind(p, value, nested);
			}

			Mapper my = mapper;
			if (my.getSetter(name) == null && bean instanceof AuditableModel) {
				my = Mapper.of(AuditableModel.class);
			}

			if (p != null) {
				if (!JPA.em().contains(bean) && p.getTarget() != null) {
					// prevent automatic association handling
					// causing detached entity exception
					my.set(bean, name, value);
				} else {
					my.getProperty(name).set(bean, value);
				}
				value = p.get(bean);
			}

			validated.put(name, value);
		}

		// first fill all the data of computed fields
		for(String name : computed) {
			mapper.getProperty(name).set(bean, data.get(name));
		}
		// then retrieve them
		for(String name : computed) {
			validated.put(name, mapper.getProperty(name).get(bean));
		}

		// create proxy to read missing values from managed instance
		bean = createProxy(bean, validated);

		return new Context(validated, bean, beanClass);
	}

	@SuppressWarnings("unchecked")
	public <T> T asType(Class<T> type) {
		Preconditions.checkArgument(type.isInstance(beanInstance),
				"Invalid type {}, should be {}",
				type.getName(), beanClass.getName());
		return (T) beanInstance;
	}

	public Class<?> getContextClass() {
		return beanClass;
	}

	public Context getParentContext() {
		return (Context) this.get(KEY_PARENT);
	}

	@Override
	public boolean containsKey(Object key) {
		if (KEY_PARENT_CONTEXT.equals(key)) {
			return true;
		}
		return super.containsKey(key);
	}

	@Override
	public Object get(Object key) {
		if (KEY_PARENT_CONTEXT.equals(key)) {
			return getParentContext();
		}
		return super.get(key);
	}

	/**
	 * Update the context with the specified key and value.<br>
	 *
	 * Use this method instead of {@link #put(String, Object)} to propagate the
	 * value to the underlying context object.
	 *
	 * @param key
	 *            key with with the context should be updated
	 * @param value
	 *            value to be associated to the given key
	 */
	public void update(String key, Object  value) {
		final Map<String, Object> values = new HashMap<>();
		values.put(key, value);
		this.update(values);
	}

	/**
	 * Update the context object with the given values.<br>
	 *
	 * Use this method instead of {@link #putAll(Map)} to propagate the values
	 * to the underlying context object.
	 *
	 */
	public void update(Map<String, Object> values) {
		if (beanInstance == null || values == null || values.isEmpty()) {
			return;
		}
		Mapper mapper = Mapper.of(beanClass);
		for(String key : values.keySet()) {
			Property property = mapper.getProperty(key);
			if (property == null) {
				continue;
			}
			property.set(beanInstance, values.get(key));
		}
		this.putAll(values);
	}
}
