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
package com.axelor.rpc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.rpc.filter.Filter;
import com.axelor.rpc.filter.JPQLFilter;
import com.axelor.rpc.filter.Operator;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Criteria {

	private Filter filter;
	
	private Map<String, Object> domainContext;

	public Criteria(Filter filter) {
		this.filter = filter;
	}
	
	public <T extends Model> Query<T> createQuery(Class<T> klass) {
		return createQuery(klass, null);
	}

	public <T extends Model> Query<T> createQuery(Class<T> klass, Filter and) {
		Query<T> q = and == null ? filter.build(klass) : Filter.and(filter, and).build(klass);
		if (domainContext != null) {
			Class<?> domainClass = klass;
			if (domainContext.containsKey("_model")) {
				try {
					domainClass = Class.forName((String) domainContext.get("_model"));
				} catch (ClassNotFoundException e) {
					//throw new RuntimeException(e);
				}
			}
			Context context = Context.create(domainContext, domainClass);
			q.bind(context);
		}
		return q;
	}

	@Override
	public String toString() {
		return filter.toString();
	}

	public static Criteria parse(Request request) {
		
		try {
			return parse(request.getData(), request.getBeanClass());
		} catch(IllegalArgumentException e) {
			System.err.println(e);
		}
		
		Map<String, Object> raw = new HashMap<String, Object>();
		List<Map<?, ?>> items = new ArrayList<Map<?, ?>>();

		raw.put("operator", "or");
		raw.put("criteria", items);
		raw.put("_domain", request.getData().get("_domain"));
		raw.put("_domainContext", request.getData().get("_domainContext"));
		
		for (String key : request.getData().keySet()) {
			
			if (!key.matches("^[a-zA-Z].*$"))
				continue;
			
			Map<String, Object> criterion = new HashMap<String, Object>();
			criterion.put("fieldName", key);
			criterion.put("operator", "like");
			criterion.put("value", request.getData().get(key));
			items.add(criterion);
		}
		return parse(raw, request.getBeanClass());
	}

	@SuppressWarnings("unchecked")
	public static Criteria parse(Map<String, Object> rawCriteria, Class<?> beanClass) {
		Filter search = Criteria.parseCriterion(rawCriteria, beanClass);
		List<Filter> all = Lists.newArrayList();
		Map<String, Object> context = Maps.newHashMap();
		
		List<?> domains = (List<?>) rawCriteria.get("_domains");
		if (domains != null) {
			all.addAll(parseDomains(domains, context));
		}

		String domain = (String) rawCriteria.get("_domain");
		if (domain != null) {
			all.add(new JPQLFilter(domain));
		}
		try {
			context.putAll((Map<String, ?>) rawCriteria.get("_domainContext"));
		} catch(Exception e){
		}

		if (!Objects.equal(Boolean.TRUE, rawCriteria.get("_archived"))) {
			all.add(new JPQLFilter("self.archived is null OR self.archived = false"));
		}
		
		if (!Strings.isNullOrEmpty(search.getQuery())) {
			all.add(search);
		}
		
		Criteria result = new Criteria(Filter.and(all));
		result.domainContext = context;

		return result;
	}

	@SuppressWarnings("all")
	private static List<Filter> parseDomains(final List<?> domains, final Map<String, ?> context) {
		final List<Filter> filters = Lists.newArrayList();
		for(final Object item : domains) {
			if (item instanceof Map && ((Map) item).containsKey("domain")) {
				final Map map = (Map) item;
				filters.add(new JPQLFilter((String) map.get("domain")));
				try {
					context.putAll((Map) map.get("context"));
				} catch (Exception e){}
			}
		}
		return filters;
	}

	@SuppressWarnings("unchecked")
	private static Filter parseCriterion(Map<String, Object> rawCriteria, Class<?> beanClass) {

		Operator operator = Operator.get((String) rawCriteria.get("operator"));

		if (operator == Operator.AND || operator == Operator.OR || operator == Operator.NOT) {
			List<Filter> filters = new ArrayList<Filter>();
			for (Object raw : (List<?>) rawCriteria.get("criteria")) {
				filters.add(parseCriterion((Map<String, Object>) raw, beanClass));
			}

			if (operator == Operator.AND)
				return Filter.and(filters);
			if (operator == Operator.OR)
				return Filter.or(filters);
			if (operator == Operator.NOT)
				return Filter.not(filters);
		}

		String fieldName = (String) rawCriteria.get("fieldName");
		Object value = rawCriteria.get("value");
		
		if (value instanceof String) {
			// use the name field of the target object in case of relational field
			Property property = Mapper.of(beanClass).getProperty(fieldName);
			if (property != null && property.getTarget() != null) {
				fieldName = fieldName + "." + Mapper.of(property.getTarget()).getNameField().getName();
			}
		}

		if (operator == Operator.BETWEEN) {
			return Filter.between(fieldName, rawCriteria.get("value"), rawCriteria.get("value2"));
		}
		
		if (value instanceof String) {
			value = ((String) value).trim();
		}

		switch (operator) {
		case EQUALS:
			return Filter.equals(fieldName, value);
		case NOT_EQUAL:
			return Filter.notEquals(fieldName, value);
		case LESS_THAN:
			return Filter.lessThen(fieldName, value);
		case GREATER_THAN:
			return Filter.greaterOrEqual(fieldName, value);
		case LESS_OR_EQUAL:
			return Filter.lessOrEqual(fieldName, value);
		case GREATER_OR_EQUAL:
			return Filter.greaterOrEqual(fieldName, value);
		case LIKE:
			return Filter.like(fieldName, value);
		case NOT_LIKE:
			return Filter.notLike(fieldName, value);
		case IS_NULL:
			return Filter.isNull(fieldName);
		case NOT_NULL:
			return Filter.notNull(fieldName);
		case IN:
			return Filter.in(fieldName, (Collection<Object>) value);
		case NOT_IN:
			return Filter.notIn(fieldName, (Collection<Object>) value);
		default:
			return Filter.equals(fieldName, value);
		}
	}
}
