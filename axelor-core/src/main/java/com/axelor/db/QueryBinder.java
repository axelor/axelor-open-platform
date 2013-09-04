/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.db;

import java.util.Map;

import javax.persistence.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.axelor.db.mapper.Adapter;
import com.google.common.collect.Maps;

public class QueryBinder {
	
	private javax.persistence.Query query;
	
	public QueryBinder(javax.persistence.Query query) {
		this.query = query;
	}
	
	public javax.persistence.Query bind(Map<String, Object> namedParams, Object[] params) {
		
		final Map<String, Object> variables = Maps.newHashMap();
		
		variables.put("__date__", new LocalDate());
		variables.put("__time__", new LocalDateTime());
		
		if (namedParams != null) {
			
			variables.putAll(namedParams);

			for (Parameter<?> p : query.getParameters()) {
				if (p.getName() == null)
					continue;
				if (variables.containsKey(p.getName())) {
					Object value = variables.get(p.getName());
					try {
						query.setParameter(p.getName(), value);
					} catch (IllegalArgumentException e) {
						query.setParameter(p.getName(), adapt(value, p));
					}
				} else {
					query.setParameter(p.getName(), adapt(null, p));
				}
			}
		}
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				Object param = params[i];
				if (param instanceof String && variables.containsKey(param)) {
					param = variables.get(param);
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
		return query;
	}
	
	private Object adapt(Object value, Parameter<?> param) {
		Class<?> type = param.getParameterType();
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