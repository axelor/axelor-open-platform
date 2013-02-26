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
		if (namedParams != null) {
			
			Map<String, Object> variables = Maps.newHashMap();
			variables.put("__date__", new LocalDate());
			variables.put("__time__", new LocalDateTime());
			
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
				try {
					query.setParameter(i + 1, params[i]);
				} catch (IllegalArgumentException e) {
					Parameter<?> p = query.getParameter(i + 1);
					query.setParameter(i + 1, adapt(params[i], p));
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