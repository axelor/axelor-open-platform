/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
			Context context = new Context(domainContext, domainClass);
			q.bind(context);
		}
		return q;
	}

	@Override
	public String toString() {
		return filter.toString();
	}

	public static Criteria parse(Request request) {

		if (request.getData() == null) {
			return null;
		}

		final Map<String, Object> data = request.getData();
		final Map<String, Object> raw = new HashMap<>();

		// default root operator
		if (data.get("operator") == null) {
			raw.put("operator", "or");
		}
		// if data itself is criteria root
		if (data.get("criteria") != null) {
			raw.putAll(data);
			return parse(raw, request.getBeanClass());
		}

		// simple search where data is field -> search value map
		raw.put("operator", "or");
		raw.put("_domain", request.getData().get("_domain"));
		raw.put("_domainContext", request.getData().get("_domainContext"));
		
		final List<Map<String, Object>> items = new ArrayList<>();
		for (String key : request.getData().keySet()) {
			if (!key.matches("^[a-zA-Z].*$")) {
				continue;
			}
			final Map<String, Object> criterion = new HashMap<String, Object>();
			criterion.put("fieldName", key);
			criterion.put("operator", "like");
			criterion.put("value", request.getData().get(key));
			items.add(criterion);
		}

		raw.put("criteria", items);

		return parse(raw, request.getBeanClass());
	}

	@SuppressWarnings("unchecked")
	public static Criteria parse(Map<String, Object> rawCriteria, Class<?> beanClass) {
		final Filter search = Criteria.parseCriterion(rawCriteria, beanClass);
		final List<Filter> all = Lists.newArrayList();
		final Map<String, Object> context = Maps.newHashMap();
		final List<?> domains = (List<?>) rawCriteria.get("_domains");

		if (domains != null) {
			all.addAll(parseDomains(domains, context));
			try {
				if (Operator.get((String) rawCriteria.get("operator")) == Operator.OR && all.size() > 0) {
					all.clear();
					all.add(Filter.or(parseDomains(domains, context)));
				}
			} catch (Exception e) {
			}
		}

		String domain = (String) rawCriteria.get("_domain");
		if (domain != null) {
			all.add(new JPQLFilter(domain));
		}
		try {
			context.putAll((Map<String, ?>) rawCriteria.get("_domainContext"));
		} catch(Exception e){
		}

		Object archived = rawCriteria.get("_archived");
		if (archived != null) {
			archived = "true".equals(archived.toString().toLowerCase());
		}
		if (archived != Boolean.TRUE) {
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
		if (operator == Operator.NOT_BETWEEN) {
			return Filter.notBetween(fieldName, rawCriteria.get("value"), rawCriteria.get("value2"));
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
			return Filter.lessThan(fieldName, value);
		case GREATER_THAN:
			return Filter.greaterThan(fieldName, value);
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
