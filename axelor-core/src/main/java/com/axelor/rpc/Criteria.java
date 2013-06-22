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
		Filter all = search;
		
		String domain = (String) rawCriteria.get("_domain");
		if (domain != null) {
			if (Strings.isNullOrEmpty(search.getQuery()))
				all = new JPQLFilter(domain);
			else
				all = Filter.and(new JPQLFilter(domain), search);
		}
		
		Criteria result = new Criteria(all);
		try {
			result.domainContext = (Map<String, Object>) rawCriteria.get("_domainContext");
		} catch(Exception e){
		}
		return result;
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
			// use the name field of the target object in case of M2O
			Property property = Mapper.of(beanClass).getProperty(fieldName);
			if (property != null && property.isReference()) {
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
