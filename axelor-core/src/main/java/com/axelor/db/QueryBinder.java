/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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
package com.axelor.db;

import java.util.Map;

import javax.persistence.FlushModeType;
import javax.persistence.Parameter;

import com.axelor.db.mapper.Adapter;
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
		query.unwrap(org.hibernate.Query.class).setCacheable(cacheable);
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

		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				Object param = params[i];
				if (param instanceof String
						&& ((String) param).startsWith("__")
						&& ((String) param).endsWith("__")
						&& bindings.containsKey(param)) {
					// special variable
					param = bindings.get(param);
				}
				try {
					query.getParameter(i + 1);
				} catch (Exception e) {
					continue;
				}
				try {
					query.setParameter(i + 1, param);
				} catch (IllegalArgumentException e) {
					Parameter<?> p = query.getParameter(i + 1);
					query.setParameter(i + 1, adapt(param, p));
				}
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

		if (value == null || value instanceof String && "".equals(((String) value).trim())) {
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
		if (value instanceof Model && type.isInstance(value)) {
			Model bean = (Model) value;
			if (bean.getId() != null)
				value = JPA.find(bean.getClass(), bean.getId());
		}
		return value;
	}
}