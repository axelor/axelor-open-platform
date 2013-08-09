package com.axelor.rpc;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
 * The bean instanced returned from the context is a detached object and should
 * never be used with JPA/Hibernate session. It's only for convenience to get the
 * context values using the bean methods.
 *
 */
public class Context extends HashMap<String, Object> {

	private static final long serialVersionUID = -5405070533303843069L;

	private Object beanInstance;

	private Context(Map<String, Object> data, Object bean) {
		super(data);
		this.beanInstance = bean;
	}

	@SuppressWarnings("all")
	public static Object createOrFind(Property p, Object value) {

		Object bean = null;

		if (value instanceof Map) {
			Map map = (Map) value;
			Object id = map.get("id");
			// if new/updated then create map
			if (map.containsKey("version") || id == null) {
				Context ctx =  create(map, p.getTarget());
				bean = ctx.beanInstance;
			} else {
				bean = JPA.find((Class) p.getTarget(), Long.parseLong(id.toString()));
			}
			if (bean != null && map.containsKey("selected"))
				Mapper.of(p.getTarget()).set(bean, "selected", map.get("selected"));
		}

		return bean;
	}

	@SuppressWarnings("all")
	public static Context create(Map<String, Object> data, Class<?> beanClass) {

		Preconditions.checkNotNull(beanClass);

		if (data == null)
			data = Maps.newHashMap();

		Mapper mapper = Mapper.of(beanClass);
		Object bean = Mapper.toBean(beanClass, null);

		List<String> computed = Lists.newArrayList();
		Map<String, Object> validated = Maps.newHashMap();

		for(String name : data.keySet()) {

			Object value = data.get(name);
			Property p = mapper.getProperty(name);

			if (p == null) {
				if ("_parent".equals(name)) {
					try {
						Class<?> parentClass = Class.forName((String) ((Map) value).get("_model"));
						value = create((Map) value, parentClass);
					} catch (Exception e) {
					}
				}
			}
			else if (p.isVirtual()) {
				computed.add(name);
				continue;
			}
			else if (p.isCollection() && value instanceof Collection) {
				List items = Lists.newArrayList();
				for(Object item : (Collection) value) {
					items.add(createOrFind(p, item));
				}
				value = items;
			}
			else if (p.isReference()) {
				value = createOrFind(p, value);
			}

			if (p != null) {
				if (!JPA.em().contains(bean) && p.isCollection()) {
					// prevent automatic association handling
					// causing detached entity exception
					mapper.set(bean, p.getName(), value);
				} else {
					p.set(bean, value);
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

		return new Context(validated, bean);
	}

	@SuppressWarnings("unchecked")
	public <T> T asType(Class<T> type) {
		Preconditions.checkArgument(type.isInstance(beanInstance),
				"Invalid type {}, should be {}",
				type.getName(), beanInstance.getClass().getName());
		return (T) beanInstance;
	}

	public Class<?> getContextClass() {
		return beanInstance.getClass();
	}

	public Context getParentContext() {
		return (Context) this.get("_parent");
	}

	@Override
	public boolean containsKey(Object key) {
		if ("parentContext".equals(key)) {
			return true;
		}
		return super.containsKey(key);
	}

	@Override
	public Object get(Object key) {
		if ("parentContext".equals(key)) {
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
		final Map<String, Object> values = Maps.newHashMap();
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
		Mapper mapper = Mapper.of(beanInstance.getClass());
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
