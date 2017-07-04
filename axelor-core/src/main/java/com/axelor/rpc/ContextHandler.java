/*
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.axelor.auth.db.AuditableModel;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.db.MetaJsonRecord;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

/**
 * The {@link ContextHandler} provides seamless way to access context values
 * using proxy.
 * 
 * <p>
 * The proxy and it's fields are initialized lazily from the context value map
 * when context variable is access. Any missing value of the bean is accessed
 * from the managed instance.
 * 
 * <p>
 * For internal use only.
 * 
 * @see Context
 */
public class ContextHandler<T> {

	private static final String FIELD_ID = "id";
	private static final String FIELD_VERSION = "version";
	private static final String FIELD_SELECTED = "selected";

	private final PropertyChangeSupport changeListeners;

	private final Map<String, Object> values;
	private final Set<String> validated;

	private final Class<T> beanClass;
	private final Mapper beanMapper;

	private T managedEntity;
	private T unmanagedEntity;
	private T proxy;

	private JsonContext jsonContext;

	private boolean searched;
	
	ContextHandler(Class<T> beanClass, Map<String, Object> values) {
		this.values = Objects.requireNonNull(values);
		this.validated = new HashSet<>();
		this.beanClass = Objects.requireNonNull(beanClass);
		this.beanMapper = Mapper.of(beanClass);
		this.changeListeners = new PropertyChangeSupport(this);
	}

	public void addChangeListener(PropertyChangeListener listener) {
		changeListeners.addPropertyChangeListener(listener);
	}

	private Long findId(Map<String, Object> values) {
		try {
			return Long.parseLong(values.get(FIELD_ID).toString());
		} catch (Exception e) {
			return null;
		}
	}

	private T getManagedEntity() {
		if (searched) {
			return managedEntity;
		}
		final Long id = findId(values);
		if (id != null) {
			managedEntity = JPA.em().find(beanClass, id);
		}
		searched = true;
		return managedEntity;
	}

	private T getUnmanagedEntity() {
		if (unmanagedEntity == null) {
			unmanagedEntity = Mapper.toBean(beanClass, null);
		}
		return unmanagedEntity;
	}

	public T getProxy() {
		return proxy;
	}

	void setProxy(T proxy) {
		this.proxy = proxy;
	}

	private JsonContext getJsonContext() {
		if (jsonContext == null) {
			jsonContext = createJsonContext();
		}
		return jsonContext;
	}

	private JsonContext createJsonContext() {
		if (MetaJsonRecord.class.isAssignableFrom(beanClass)) {
			final MetaJsonRecord rec = (MetaJsonRecord) proxy;
			return new JsonContext(rec);
		}
		final Property p = beanMapper.getProperty(Context.KEY_JSON_ATTRS);
		final Context c = new Context(beanClass);
		return new JsonContext(c, p, (String) p.get(proxy));
	}

	@SuppressWarnings("unchecked")
	private Object createOrFind(Property property, Object item) {
		if (item == null || item instanceof Model) {
			return item;
		}
		if (item instanceof Map) {
			final Map<String, Object> map = (Map<String, Object>) item;
			final Long id = findId(map);
			// if new or updated, create proxy
			if (id == null || id <= 0 || map.containsKey(FIELD_VERSION)) {
				return ContextHandlerFactory.newHandler(property.getTarget(), map).getProxy();
			}
			// use managed instance
			final Object bean = JPA.em().find(property.getTarget(), id);
			if (map.containsKey(FIELD_SELECTED)) {
				Mapper.of(property.getTarget()).set(bean, FIELD_SELECTED, map.get(FIELD_SELECTED));
			}
			return bean;
		}
		if (item instanceof Number) {
			return JPA.em().find(property.getTarget(), item);
		}
		throw new IllegalArgumentException("Invalid collection item for field: " + property.getName());
	}
	
	private void validate(Property property) {
		if (property == null
				|| validated.contains(property.getName())
				|| !values.containsKey(property.getName())) {
			return;
		}
		Object value = values.get(property.getName());
		if (property.isCollection() && value instanceof Collection) {
			value = ((Collection<?>) value).stream()
					.map(item -> createOrFind(property, item))
					.collect(Collectors.toList());
		} else if (property.isReference()) {
			value = createOrFind(property, value);
		}

		final Object bean = getUnmanagedEntity();

		Mapper mapper = beanMapper;
		if (mapper.getSetter(property.getName()) == null && bean instanceof AuditableModel) {
			mapper = Mapper.of(AuditableModel.class);
		}

		// prevent automatic association handling
		// causing detached entity exception
		mapper.set(bean, property.getName(), value);

		validated.add(property.getName());
	}

	private Object interceptComputeAccess(Callable<?> superCall, Method method, Object[] args) throws Exception {
		final Property computed = beanMapper.getProperty(method);
		final Set<String> depends;
		if (computed == null || (depends = beanMapper.getComputeDependencies(computed)) == null || depends.isEmpty()) {
			return superCall.call();
		}

		for (String name : depends) {
			final Property property;
			if (validated.contains(name) || (property = beanMapper.getProperty(name)) == null) {
				continue;
			}
			if (values.containsKey(name)) {
				validate(property);
			} else {
				beanMapper.set(getUnmanagedEntity(), name, property.get(getManagedEntity()));
			}
		}

		method.setAccessible(true);
		return method.invoke(getUnmanagedEntity(), args);
	}

	public Object interceptJsonAccess(Method method, Object[] args) throws Exception {
		switch (method.getName()) {
		case "get":
		case "put":
			final String name = (String) args[0];
			final Method found = args.length == 2
					? beanMapper.getSetter(name)
					: beanMapper.getGetter(name);
			if (found == null) {
				return method.invoke(getJsonContext(), args);
			}
			final Object[] params = args.length == 2
					? new Object[]{ args[1] }
					: new Object[]{};
			return found.invoke(proxy, params);
		}
		throw new UnsupportedOperationException("cannot call '" + method + "' on proxy object");
	}
	
	@RuntimeType
	public Object intercept(@SuperCall Callable<?> superCall, @Origin Method method, @AllArguments Object[] args) throws Throwable {

		// if map access (for json values)
		if (superCall == null && method.getDeclaringClass() == Map.class) {
			return interceptJsonAccess(method, args);
		}

		// handle compute method calls
		if (Modifier.isProtected(method.getModifiers())) {
			return interceptComputeAccess(superCall, method, args);
		}

		final Property property = beanMapper.getProperty(method);
		// no fields defined or is computed field
		if (property == null || property.isVirtual()) {
			return superCall.call();
		}

		final String fieldName = property.getName();

		// in case of setter, update context map
		final Object oldValue = args.length == 1
				? values.put(fieldName, args[0])
				: null;
	
		// if setter or value found in context map for the getter
		if (args.length == 1 || values.containsKey(fieldName) || property.isTransient()) {
			validate(property);
			try {
				return method.invoke(getUnmanagedEntity(), args);
			} finally {
				if (args.length == 1 && changeListeners.hasListeners(fieldName)) {
					changeListeners.firePropertyChange(fieldName, oldValue, values.get(fieldName));
				}
			}
		}
		// else get value from managed instance
		final Object managed = getManagedEntity();
		if (managed == null) {
			return null;
		}

		return method.invoke(managed, args);
	}

	@RuntimeType
	public Object getContextEntity() {
		final Object bean = getUnmanagedEntity();
		final Object managed = getManagedEntity();

		// populate the bean
		for (Property property : beanMapper.getProperties()) {
			this.validate(property);
			if (managed != null && property.isVirtual()) {
				final Set<String> depends = beanMapper.getComputeDependencies(property);
				if (depends != null) {
					depends.stream()
						.filter(n -> !validated.contains(n))
						.forEach(n -> beanMapper.set(bean, n, beanMapper.get(managed, n)));
				}
			}
		}

		// make sure to have version value
		if (managed != null && bean instanceof Model && !values.containsKey(FIELD_VERSION)) {
			((Model) bean).setVersion(((Model) managed).getVersion());
		}

		return bean;
	}
}
