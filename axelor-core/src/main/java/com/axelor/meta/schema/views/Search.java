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
package com.axelor.meta.schema.views;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.persistence.Query;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.QueryBinder;
import com.axelor.db.internal.DBHelper;
import com.axelor.db.mapper.Adapter;
import com.axelor.db.mapper.Mapper;
import com.axelor.i18n.I18n;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

@XmlType
@JsonTypeName("search")
public class Search extends AbstractView {

	@XmlAttribute
	private Integer limit;

	@XmlAttribute(name = "search-form")
	private String searchForm;

	@XmlElement(name = "field", type = SearchField.class)
	@XmlElementWrapper(name = "search-fields")
	private List<SearchField> searchFields;

	@XmlElement(name = "field", type = SearchField.class)
	@XmlElementWrapper(name = "result-fields")
	private List<SearchField> resultFields;

	@XmlElement(name = "select")
	private List<SearchSelect> selects;

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}
	
	public String getSearchForm() {
		return searchForm;
	}
	
	public void setSearchForm(String searchForm) {
		this.searchForm = searchForm;
	}

	public List<SearchField> getSearchFields() {
		return searchFields;
	}

	public void setSearchFields(List<SearchField> searchFields) {
		this.searchFields = searchFields;
	}

	public SearchField getSearchField(String name) {
		for(SearchField field : searchFields) {
			if (name.equals(field.getName()))
				return field;
		}
		return null;
	}

	public List<SearchField> getResultFields() {
		return resultFields;
	}

	public void setResultFields(List<SearchField> resultFields) {
		this.resultFields = resultFields;
	}

	public List<SearchSelect> getSelects() {
		return selects;
	}

	public void setSelects(List<SearchSelect> selects) {
		this.selects = selects;
	}

	@XmlType
	@JsonInclude(Include.NON_NULL)
	public static class SearchField extends Field {

		@XmlAttribute
		private Boolean multiple;

		public Boolean getMultiple() {
			return multiple;
		}

		@JsonGetter("type")
		@Override
		public String getServerType() {
			return super.getServerType();
		}

		public static Map<String, Class<?>> getTypes() {
			return TYPES;
		}

		private static final Map<String, Class<?>> TYPES = new ImmutableMap.Builder<String, Class<?>>()
			.put("string", String.class)
			.put("integer", Integer.class)
			.put("decimal", BigDecimal.class)
			.put("date", LocalDate.class)
			.put("datetime", LocalDateTime.class)
			.put("boolean", Boolean.class)
			.build();

		@SuppressWarnings("rawtypes")
		public Object validate(Object input) {
			try {
				Class<?> klass = (Class<?>) TYPES.get(getServerType());
				if ("reference".equals(getServerType())) {
					klass = Class.forName(getTarget());
					if(input != null) {
						return JPA.em().find(klass, Long.valueOf(((Map)input).get("id").toString()));
					}
				}
				if(klass != null) {
					return Adapter.adapt(input, klass, klass, null);
				}
			} catch (Exception e) {
			}
			return input;
		}
	}

	public ScriptHelper scriptHandler(Map<String, Object> variables) {
		Map<String, Object> map = Maps.newHashMap(variables);
		for(SearchField field : searchFields) {
			map.put(field.getName(), field.validate(variables.get(field.getName())));
		}
		return new CompositeScriptHelper(new ScriptBindings(map));
	}

	static class JoinHelper {

		private Map<String, String> joins = Maps.newLinkedHashMap();

		public String joinName(String name) {

			String[] path = name.split("\\.");

			String prefix = null;
			String variable = name;

			if (path.length > 1) {
				variable = path[path.length - 1];
				String joinOn = null;
				for(int i = 0 ; i < path.length - 1 ; i++) {
					String item = path[i].replace("[]", "");
					if (prefix == null) {
						joinOn = "self." + item;
						prefix = "_" + item;
					} else {
						joinOn = prefix + "." + item;
						prefix = prefix + "_" + item;
					}
					if (!joins.containsKey(joinOn)) {
						joins.put(joinOn, prefix);
					}
				}
			}

			if (prefix == null) {
				prefix = "self";
			}

			return prefix + "." + variable;
		}

		@Override
		public String toString() {
			if (joins.size() == 0) return "";
			List<String> joinItems = Lists.newArrayList();
			for(String key : joins.keySet()) {
				String val = joins.get(key);
				joinItems.add("LEFT JOIN " + key + " " + val);
			}
			return Joiner.on(" ").join(joinItems);
		}
	}

	@XmlType
	public static class SearchSelect {

		@XmlAttribute
		private String model;

		@XmlAttribute
		private String title;
		
		@XmlAttribute(name = "view-title")
		private String viewTitle;

		@XmlAttribute
		private Boolean selected;

		@JsonIgnore
		@XmlAttribute
		private String orderBy;

		@JsonIgnore
		@XmlAttribute(name = "if")
		private String condition;

		@XmlAttribute(name = "form-view")
		private String formView;

		@XmlAttribute(name = "grid-view")
		private String gridView;

		@JsonIgnore
		@XmlElement(name = "field")
		private List<SearchSelectField> fields;

		@JsonIgnore
		@XmlElement
		private SearchSelectWhere where;

		private transient String queryString;

		public String getQueryString() {
			return queryString;
		}

		public String getModel() {
			return model;
		}

		@JsonGetter("title")
		public String getLocalizedTitle() {
			if (title == null && model != null) {
				return model.substring(model.lastIndexOf('.') + 1);
			}
			return I18n.get(title);
		}

		@JsonIgnore
		public String getTitle() {
			return title;
		}

		public String getViewTitle() {
			return viewTitle;
		}
		
		public Boolean getSelected() {
			return selected;
		}

		public String getOrderBy() {
			return orderBy;
		}

		public String getCondition() {
			return condition;
		}

		public String getFormView() {
			return formView;
		}

		public String getGridView() {
			return gridView;
		}

		public List<SearchSelectField> getFields() {
			return fields;
		}

		public SearchSelectWhere getWhere() {
			return where;
		}

		public Query toQuery(Search search, ScriptHelper scriptHelper) throws ClassNotFoundException {

			if (!scriptHelper.test(condition))
				return null;

			Class<?> klass = Class.forName(getModel());
			List<String> selection = Lists.newArrayList("self.id AS id", "self.version AS version");
			JoinHelper joinHelper = new JoinHelper();

			for(SearchSelectField field : fields) {
				String name = field.getName();
				String as = field.getAs();
				name = joinHelper.joinName(name);
				selection.add(String.format("%s AS %s", name, as));
			}

			StringBuilder builder = new StringBuilder();
			builder.append("SELECT new map(");
			Joiner.on(", ").appendTo(builder, selection);
			builder.append(")");
			builder.append(" FROM ").append(klass.getSimpleName()).append(" self");

			Map<String, Object> binding = where.build(builder, joinHelper, scriptHelper);
			if (binding == null || binding.size() == 0)
				return null;

			List<String> orders = Lists.newArrayList();
			if (orderBy != null) {
				for (String spec : Splitter.on(Pattern.compile(",\\s*")).split(orderBy)) {
					if (spec.startsWith("-")) orders.add(spec.substring(1) + " DESC");
					else orders.add(spec);
				};
			}

			if (orders.size() > 0) {
				builder.append(" ORDER BY ");
				Joiner.on(", ").appendTo(builder, orders);
			}

			queryString = builder.toString();

			Query query = JPA.em().createQuery(queryString);
			QueryBinder.of(query).bind(binding);

			return query;
		}
	}

	@XmlType
	public static class SearchSelectField {

		@XmlAttribute
		private String name;

		@XmlAttribute
		private String as;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getAs() {
			return as;
		}

		public void setAs(String as) {
			this.as = as;
		}
	}

	@XmlType
	public static class SearchSelectWhere {

		@XmlAttribute
		private String match;

		@XmlAttribute
		private Boolean showArchived;

		@XmlElement(name = "input")
		private List<SearchSelectInput> inputs;

		public String getMatch() {
			return match;
		}

		public Boolean getShowArchived() {
			return showArchived;
		}

		public List<SearchSelectInput> getInputs() {
			return inputs;
		}

		@SuppressWarnings("rawtypes")
		private Object getValue(SearchSelectInput input, ScriptHelper handler) {
			Object value = null;
			String[] names = input.getName().split("\\.");

			value = handler.getBindings().get(names[0]);
			if(input.getExpression() != null) { return handler.eval(input.getExpression()); }
			if (value == null || names.length == 1) { return value; }

			for(int i = 1 ; i < names.length ; i ++) {
				if (value instanceof Map) {
					value = ((Map) value).get(names[i]);
				} else if (value instanceof Model){
					Mapper mapper = Mapper.of(value.getClass());
					value = mapper.get(value, names[i]);
				}
			}
			return value;
		}

		Map<String, Object> build(StringBuilder builder, JoinHelper joinHelper, ScriptHelper handler) {

			List<String> where = Lists.newArrayList();
			Map<String, Object> binding = Maps.newHashMap();
			Multimap<String, String> groups = HashMultimap.create();
			boolean filterArchived = showArchived != Boolean.TRUE;

			String join = "any".equals(match) ? " OR " : " AND ";

			for(SearchSelectInput input : inputs) {

				if (!handler.test(input.condition))
					continue;

				String name = input.getField();
				String as = input.getName();
				Object value = this.getValue(input, handler);

				if (value != null) {

					name = joinHelper.joinName(name);

					String left = "LOWER(" + name + ")";
					String operator = "LIKE";

					if ("contains".equals(input.matchStyle)) {
						value = "%" + value.toString().toLowerCase() + "%";
					}
					else if ("startsWith".equals(input.matchStyle)) {
						value = value.toString().toLowerCase() + "%";
					}
					else if ("endsWith".equals(input.matchStyle)) {
						value = "%" + value.toString().toLowerCase();
					}
					else if ("lessThan".equals(input.matchStyle)) {
						operator = "<";
						left = name;
					}
					else if ("greaterThan".equals(input.matchStyle)) {
						operator = ">";
						left = name;
					}
					else if ("lessOrEqual".equals(input.matchStyle)) {
						operator = "<=";
						left = name;
					}
					else if ("greaterOrEqual".equals(input.matchStyle)) {
						operator = ">=";
						left = name;
					}
					else if ("notEquals".equals(input.matchStyle)) {
						operator = "!=";
						left = name;
					}
					else {
						operator = "=";
						left = name;
					}

					if ("archived".equals(input.getField())) {
						filterArchived = value != Boolean.TRUE;
						if (filterArchived) {
							continue;
						}
					}

					String filter = null;
					String first = as.split("\\.")[0];
					as = as.replace('.', '_');

					if ("LIKE".equals(operator) && DBHelper.isUnaccentEnabled()) {
						filter = String.format("unaccent(%s) %s unaccent(:%s)", left, operator, as);
					} else {
						filter = String.format("%s %s :%s", left, operator, as);
					}

					binding.put(as, value);
					groups.put(first, filter);
				}
			}

			for(Collection<String> items : groups.asMap().values()) {
				String clause = Joiner.on(" OR ").join(items);
				if (items.size() > 1) {
					clause = "(" + clause + ")";
				}
				where.add(clause);
			}

			if (where.size() > 0) {
				builder.append(" ").append(joinHelper.toString());
			}

			builder.append(" WHERE ");

			if (filterArchived) {
				builder.append("(self.archived is null OR self.archived = false)");
			}

			if (where.size() > 0) {
				if (filterArchived) {
					builder.append(" AND (");
				}
				Joiner.on(join).appendTo(builder, where);
				if (filterArchived) {
					builder.append(")");
				}
				return binding;
			}

			return null;
		}
	}

	@XmlType
	public static class SearchSelectInput {

		@XmlAttribute
		private String name;

		@XmlAttribute
		private String field;

		@XmlAttribute
		private String matchStyle;

		@XmlAttribute(name = "if")
		private String condition;
		
		@XmlAttribute(name = "expr")
		private String expression;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}

		public String getMatchStyle() {
			return matchStyle;
		}

		public void setMatchStyle(String matchStyle) {
			this.matchStyle = matchStyle;
		}

		public String getCondition() {
			return condition;
		}

		public void setCondition(String condition) {
			this.condition = condition;
		}
		
		public String getExpression() {
			return expression;
		}
		
		public void setExpression(String expression) {
			this.expression = expression;
		}
	}
}
