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
package com.axelor.db;

import java.util.Map;

import javax.persistence.FlushModeType;
import javax.persistence.Parameter;

import com.axelor.common.StringUtils;
import com.axelor.db.mapper.Adapter;
import com.axelor.rpc.ContextEntity;
import com.axelor.script.ScriptBindings;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

/**
 * The query binder class provides the helper methods to bind query parameters
 * and mark the query cacheable.
 *
 */
public class QueryBinder {

	private final javax.persistence.Query query;
	
	/**
	 * Create a new query binder for the given query instance.
	 *
	 * @param query
	 *            the query instance
	 */
	private QueryBinder(javax.persistence.Query query) {
		this.query = query;
	}

	/**
	 * Create a new query binder for the given query instance.
	 *
	 * @param query
	 *            the query instance
	 *
	 * @return a new query binder instance
	 */
	public static QueryBinder of(javax.persistence.Query query) {
		return new QueryBinder(query);
	}

	/**
	 * Set the query cacheable.
	 *
	 * @return the same query binder instance
	 */
	public QueryBinder setCacheable() {
		return this.setCacheable(true);
	}

	/**
	 * Set whether to set the query cacheable or not.
	 *
	 * @param cacheable
	 *            whether to set cacheable or not
	 * @return the same query binder instance
	 */
	public QueryBinder setCacheable(boolean cacheable) {
		query.setHint("org.hibernate.cacheable", "" + cacheable);
		return this;
	}
	
	/**
	 * Set the query readOnly.
	 *
	 * @return the same query binder instance
	 */
	public QueryBinder setReadOnly() {
		return setReadOnly(true);
	}

	/**
	 * Set the query readOnly.
	 * 
	 * <p>
	 * This will give better performance if the result is not meant for updates.
	 * For example, REST api data fetching can benefit from this.
	 * </p>
	 * 
	 * @return the same query binder instance
	 */
	public QueryBinder setReadOnly(boolean readOnly) {
		query.unwrap(org.hibernate.query.Query.class).setReadOnly(readOnly);
		return this;
	}

	/**
	 * Set query flush mode.
	 *
	 * @param mode
	 *            flush mode
	 * @return the same query binder instance
	 */
	public QueryBinder setFlushMode(FlushModeType mode) {
		query.setFlushMode(mode);
		return this;
	}

	/**
	 * Shortcut to the {@link #setCacheable()} and
	 * {@link #setFlushMode(FlushModeType)} methods.
	 *
	 * @param cacheable
	 *            whether to mark the query cacheable
	 * @param type
	 *            the {@link FlushModeType}, only set if type is not null
	 * @return the same query binder instance
	 */
	public QueryBinder opts(boolean cacheable, FlushModeType type) {
		this.setCacheable(cacheable);
		if (type != null) {
			this.setFlushMode(type);
		}
		return this;
	}

	/**
	 * Bind the query with the given named and/or positional parameters.
	 *
	 * The parameter values will be automatically adapted to correct data type
	 * of the query parameter.
	 *
	 * @param namedParams
	 *            the named parameters
	 * @param params
	 *            the positional parameters
	 *
	 * @return the same query binder instance
	 */
	public QueryBinder bind(Map<String, Object> namedParams, Object... params) {

		ScriptBindings bindings = null;

		if (namedParams instanceof ScriptBindings) {
			bindings = (ScriptBindings) namedParams;
		} else {
			Map<String, Object> variables = Maps.newHashMap();
			if (namedParams != null) {
				variables.putAll(namedParams);
			}
			bindings = new ScriptBindings(variables);
		}

		if (namedParams != null) {
			for (Parameter<?> p : query.getParameters()) {
				if (p.getName() != null && Ints.tryParse(p.getName()) == null) {
					this.bind(p.getName(), bindings.get(p.getName()));
				}
			}
		}
		
		if (params == null) {
			return this;
		}

		// check if we have 1 based positional params
		int offset = 0;
		try {
			query.getParameter(0);
			// TODO: enforce JPA style positional params
		} catch (Exception e) {
			offset = 1;
		}

		for (int i = 0; i < params.length; i++) {
			Parameter<?> param;
			Object value = params[i];
			if (value instanceof ContextEntity) {
				value = ((ContextEntity) value).getContextId();
			} else if (value instanceof Model) {
				value = ((Model) value).getId();
			} else if (value instanceof String
					&& !StringUtils.isBlank((String) value)
					&& bindings.containsKey(value)) {
				value = bindings.get(value);
			}
			try {
				param = query.getParameter(i + offset);
			} catch (Exception e) {
				continue;
			}
			try {
				query.setParameter(i + offset, value);
			} catch (IllegalArgumentException e) {
				query.setParameter(i + offset, adapt(value, param));
			}
		}

		return this;
	}

	/**
	 * Bind the given named parameter with the given value.
	 *
	 * @param name
	 *            the named parameter
	 * @param value
	 *            the parameter value
	 * @return the same query binder instance
	 */
	public QueryBinder bind(String name, Object value) {
		Parameter<?> parameter = null;
		try {
			parameter = query.getParameter(name);
		} catch (Exception e) {}

		if (parameter == null) {
			return this;
		}

		if (value instanceof ContextEntity) {
			value = ((ContextEntity) value).getContextId();
		} else if (value instanceof Model) {
			value = ((Model) value).getId();
		} else if (value == null || value instanceof String && "".equals(((String) value).trim())) {
			value = adapt(value, parameter);
		}

		try {
			query.setParameter(name, value);
		} catch (IllegalArgumentException e) {
			query.setParameter(name, adapt(value, parameter));
		}

		return this;
	}

	/**
	 * Get the underlying query instance.
	 *
	 * @return the query instance
	 */
	public javax.persistence.Query getQuery() {
		return query;
	}
	
	private Object adapt(Object value, Parameter<?> param) {
		final Class<?> type = param.getParameterType();
		if (type == null) {
			return value;
		}

		value = Adapter.adapt(value, type, type, null);

		if (value instanceof Number && Model.class.isAssignableFrom(type)) {
			value = JPA.em().find(type, value);
		} else if (value instanceof Model && type.isInstance(value)) {
			Model bean = (Model) value;
			if (bean.getId() != null) {
				value = JPA.find(bean.getClass(), bean.getId());
			}
		}

		return value;
	}
}
