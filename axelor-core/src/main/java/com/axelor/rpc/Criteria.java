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
import com.google.common.base.Strings;

public class Criteria {

	private Filter filter;
	
	private Map<String, Object> domainContext;

	public Criteria(Filter filter) {
		this.filter = filter;
	}
	
	public <T extends Model> Query<T> createQuery(Class<T> klass) {
		Query<T> q = filter.build(klass);
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
		
		String operator = request.getTextMatchStyle();
		if (operator == null) {
			operator = Request.TEXT_MATCH_EXACT;
		}
		if (operator.equals(Request.TEXT_MATCH_EXACT))
			operator = "equals";
		if (operator.equals(Request.TEXT_MATCH_SUBSTRING))
			operator = "contains";

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
			criterion.put("operator", operator);
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

		OperatorId op = OperatorId.get((String) rawCriteria.get("operator"));

		if (op == OperatorId.AND || op == OperatorId.OR || op == OperatorId.NOT) {
			List<Filter> filters = new ArrayList<Filter>();
			for (Object raw : (List<?>) rawCriteria.get("criteria")) {
				filters.add(parseCriterion((Map<String, Object>) raw, beanClass));
			}

			if (op == OperatorId.AND)
				return Filter.and(filters);
			if (op == OperatorId.OR)
				return Filter.or(filters);
			if (op == OperatorId.NOT)
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

		if (op == OperatorId.BETWEEN || op == OperatorId.BETWEEN_INCLUSIVE) {
			return Filter.between(fieldName, rawCriteria.get("start"),
					rawCriteria.get("end"));
		}
		
		if (value instanceof String) {
			value = ((String) value).trim();
		}

		switch (op) {
		case EQUALS:
		case IEQUALS:
			return Filter.equals(fieldName, value);
		case NOT_EQUAL:
		case INOT_EQUAL:
			return Filter.notEquals(fieldName, value);
		case LESS_THAN:
			return Filter.lessThen(fieldName, value);
		case GREATER_THAN:
			return Filter.greaterOrEqual(fieldName, value);
		case LESS_OR_EQUAL:
			return Filter.lessOrEqual(fieldName, value);
		case GREATER_OR_EQUAL:
			return Filter.greaterOrEqual(fieldName, value);
		case CONTAINS:
		case ICONTAINS:
			value = "%" + value + "%";
		case STARTS_WITH:
		case ISTARTS_WITH:
			value = value + "%";
		case ENDS_WITH:
		case IENDS_WITH:
			value = "%" + value;
			return Filter.like(fieldName, value);
		case NOT_CONTAINS:
		case INOT_CONTAINS:
			value = "%" + value + "%";
		case NOT_STARTS_WITH:
		case INOT_STARTS_WITH:
			value = value + "%";
		case NOT_ENDS_WITH:
		case INOT_ENDS_WITH:
			value = "%" + value;
			return Filter.notLike(fieldName, value);
		case IS_NULL:
			return Filter.isNull(fieldName);
		case NOT_NULL:
			return Filter.notNull(fieldName);
		case IN_SET:
			return Filter.in(fieldName, (Collection<Object>) value);
		case NOT_IN_SET:
			return Filter.notIn(fieldName, (Collection<Object>) value);
		default:
			return Filter.equals(fieldName, value);
		}
	}
}
