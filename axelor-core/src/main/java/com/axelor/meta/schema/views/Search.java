/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.mapper.Adapter;
import com.axelor.db.mapper.Mapper;
import com.axelor.i18n.I18n;
import com.axelor.meta.MetaStore;
import com.axelor.rpc.filter.Filter;
import com.axelor.rpc.filter.JPQLFilter;
import com.axelor.rpc.filter.Operator;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeName("search")
public class Search extends AbstractView {

  @XmlAttribute private Integer limit;

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
    for (SearchField field : searchFields) {
      if (name.equals(field.getName())) return field;
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

    @XmlAttribute private Boolean multiple;

    public Boolean getMultiple() {
      return multiple;
    }

    public void setMultiple(Boolean multiple) {
      this.multiple = multiple;
    }

    @JsonGetter("type")
    @Override
    public String getServerType() {
      return super.getServerType();
    }

    public static Map<String, Class<?>> getTypes() {
      return TYPES;
    }

    private static final Map<String, Class<?>> TYPES =
        new ImmutableMap.Builder<String, Class<?>>()
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
        Class<?> klass = TYPES.get(getServerType());
        if ("reference".equals(getServerType())) {
          klass = Class.forName(getTarget());
          if (input != null) {
            return JPA.em().find(klass, Long.valueOf(((Map) input).get("id").toString()));
          }
        }
        if ("enum".equals(getServerType())) {
          return input;
        }
        if (klass != null && BigDecimal.class.isAssignableFrom(klass) && input == null) {
          return null;
        }
        if (klass != null) {
          return Adapter.adapt(input, klass, klass, null);
        }
      } catch (Exception e) {
        // ignore
      }
      return input;
    }
  }

  public ScriptHelper scriptHandler(Map<String, Object> variables) {
    Map<String, Object> map = Maps.newHashMap(variables);
    for (SearchField field : searchFields) {
      map.put(field.getName(), field.validate(variables.get(field.getName())));
    }
    return new CompositeScriptHelper(new ScriptBindings(map));
  }

  @XmlType
  public static class SearchSelect {

    @XmlAttribute private String model;

    @XmlAttribute private String title;

    @XmlAttribute(name = "view-title")
    private String viewTitle;

    @XmlAttribute private Boolean selected;

    @JsonIgnore @XmlAttribute private String orderBy;

    @JsonIgnore
    @XmlAttribute(name = "if")
    private String condition;

    @XmlAttribute(name = "form-view")
    private String formView;

    @XmlAttribute(name = "grid-view")
    private String gridView;

    @XmlElement(name = "field")
    private List<SearchSelectField> fields;

    @JsonIgnore @XmlElement private SearchSelectWhere where;

    @XmlAttribute private Integer limit;

    @XmlAttribute private Boolean distinct;

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Model> getModelClass() {
      try {
        return (Class<Model>) Class.forName(model);
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }
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

    public void setTitle(String title) {
      this.title = title;
    }

    public String getViewTitle() {
      return viewTitle;
    }

    public void setViewTitle(String viewTitle) {
      this.viewTitle = viewTitle;
    }

    public Boolean getSelected() {
      return selected;
    }

    public void setSelected(Boolean selected) {
      this.selected = selected;
    }

    public String getOrderBy() {
      return orderBy;
    }

    public void setOrderBy(String orderBy) {
      this.orderBy = orderBy;
    }

    public String getCondition() {
      return condition;
    }

    public void setCondition(String condition) {
      this.condition = condition;
    }

    public String getFormView() {
      return formView;
    }

    public void setFormView(String formView) {
      this.formView = formView;
    }

    public String getGridView() {
      return gridView;
    }

    public void setGridView(String gridView) {
      this.gridView = gridView;
    }

    public List<SearchSelectField> getFields() {
      return fields;
    }

    public void setFields(List<SearchSelectField> fields) {
      this.fields = fields;
    }

    public SearchSelectWhere getWhere() {
      return where;
    }

    public void setWhere(SearchSelectWhere where) {
      this.where = where;
    }

    public void setLimit(Integer limit) {
      this.limit = limit;
    }

    public Integer getLimit() {
      return limit;
    }

    public Boolean getDistinct() {
      return distinct;
    }

    public void setDistinct(Boolean distinct) {
      this.distinct = distinct;
    }

    public Query<?>.Selector toQuery(ScriptHelper scriptHelper) {

      if (!scriptHelper.test(condition)) return null;

      Class<? extends Model> klass = getModelClass();

      List<Filter> all = Lists.newArrayList();
      Filter filter = where.build(scriptHelper);
      if (filter == null) {
        return null;
      }

      boolean hasArchivedFilter = !Boolean.TRUE.equals(where.getShowArchived());
      if (hasArchivedFilter) {
        all.add(new JPQLFilter("self.archived IS NULL OR self.archived = FALSE"));
      }
      all.add(filter);

      Query<?> query = Filter.and(all).build(klass);
      if (orderBy != null) {
        Splitter.on(Pattern.compile(",\\s*")).split(orderBy).forEach(query::order);
      }

      return query.select(
          fields.stream()
              .map(SearchSelectField::getName)
              .collect(Collectors.toList())
              .toArray(new String[] {}));
    }
  }

  @XmlType
  public static class SearchSelectField {

    @XmlAttribute private String name;

    @XmlAttribute private String as;

    @JsonIgnore
    @XmlAttribute
    private String selection;

    @JsonIgnore
    @XmlAttribute(name = "enum-type")
    private String enumType;

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

    public void setSelection(String selection) {
      this.selection = selection;
    }

    public String getSelection() {
      return selection;
    }

    public void setEnumType(String enumType) {
      this.enumType = enumType;
    }

    public String getEnumType() {
      return enumType;
    }

    @JsonGetter("selectionList")
    public List<Selection.Option> getSelectionList() {
      if (StringUtils.notBlank(enumType)) {
        try {
          return MetaStore.getSelectionList(Class.forName(enumType));
        } catch (ClassNotFoundException e) {
          throw new RuntimeException("No such enum type found: " + enumType, e);
        }
      }

      if (StringUtils.notBlank(selection)) {
        return MetaStore.getSelectionList(selection);
      }

      return null;
    }
  }

  @XmlType
  @XmlEnum
  public enum SearchSelectWhereMatch {
    @XmlEnumValue("any")
    ANY(Operator.OR),
    @XmlEnumValue("all")
    ALL(Operator.AND);

    private final Operator operator;

    SearchSelectWhereMatch(Operator operator) {
      this.operator = operator;
    }

    public Operator getOperator() {
      return operator;
    }
  }

  @XmlType
  public static class SearchSelectWhere {

    @XmlAttribute private SearchSelectWhereMatch match;

    @XmlAttribute private Boolean showArchived;

    @XmlAttribute(name = "if")
    private String condition;

    @XmlElement(name = "input")
    private List<SearchSelectInput> inputs;

    @XmlElement(name = "where")
    private List<SearchSelectWhere> wheres;

    public SearchSelectWhereMatch getMatch() {
      return match;
    }

    public void setMatch(SearchSelectWhereMatch match) {
      this.match = match;
    }

    public Boolean getShowArchived() {
      return showArchived;
    }

    public void setShowArchived(Boolean showArchived) {
      this.showArchived = showArchived;
    }

    public String getCondition() {
      return condition;
    }

    public void setCondition(String condition) {
      this.condition = condition;
    }

    public List<SearchSelectInput> getInputs() {
      return inputs;
    }

    public void setInputs(List<SearchSelectInput> inputs) {
      this.inputs = inputs;
    }

    public List<SearchSelectWhere> getWheres() {
      return wheres;
    }

    public void setWheres(List<SearchSelectWhere> wheres) {
      this.wheres = wheres;
    }

    @SuppressWarnings("rawtypes")
    private Object getValue(SearchSelectInput input, ScriptHelper handler) {
      Object value = null;
      String[] names = input.getName().split("\\.");

      value = handler.getBindings().get(names[0]);
      if (input.getExpression() != null) {
        return handler.eval(input.getExpression());
      }
      if (value == null || names.length == 1) {
        return value;
      }

      for (int i = 1; i < names.length; i++) {
        if (value instanceof Map) {
          value = ((Map) value).get(names[i]);
        } else if (value instanceof Model) {
          Mapper mapper = Mapper.of(value.getClass());
          value = mapper.get(value, names[i]);
        }
      }
      return value;
    }

    Filter build(ScriptHelper handler) {

      if (!handler.test(condition)) {
        return null;
      }

      List<Filter> filters = Lists.newArrayList();
      if (ObjectUtils.notEmpty(inputs)) {
        for (SearchSelectInput input : inputs) {

          if (!handler.test(input.condition)) continue;

          String name = input.getField();
          Object value = this.getValue(input, handler);

          if (value != null) {

            if (value instanceof String) {
              value = ((String) value).trim();
            }

            Filter filter;
            SearchSelectInputMatchStyle matchStyle = input.getMatchStyle();
            if(matchStyle == null) {
              matchStyle = SearchSelectInputMatchStyle.EQUALS;
            }

            switch (matchStyle) {
              case CONTAINS:
                filter = Filter.like(name, value);
                break;
              case STARTS_WITH:
                filter = Filter.like(name, value + "%");
                break;
              case ENDS_WITH:
                filter = Filter.like(name, "%" + value);
                break;
              case LESS_THAN:
                filter = Filter.lessThan(name, value);
                break;
              case GREATER_THAN:
                filter = Filter.greaterThan(name, value);
                break;
              case LESS_OR_EQUAL:
                filter = Filter.lessOrEqual(name, value);
                break;
              case GREATER_OR_EQUALS:
                filter = Filter.greaterOrEqual(name, value);
                break;
              case NOT_EQUALS:
                filter = Filter.notEquals(name, value);
                break;
              default:
                filter = Filter.equals(name, value);
                break;
            }

            filters.add(filter);
          }
        }
      }

      if (ObjectUtils.notEmpty(wheres)) {
        for (SearchSelectWhere subWhere : wheres) {
          Filter subFilters = subWhere.build(handler);
          if (subFilters == null) {
            continue;
          }
          filters.add(subFilters);
        }
      }

      if (ObjectUtils.isEmpty(filters)) {
        return null;
      }

      SearchSelectWhereMatch whereMatch = match;
      if (whereMatch == null) {
        whereMatch = SearchSelectWhereMatch.ALL;
      }

      if (Operator.OR == whereMatch.getOperator()) {
        return Filter.or(filters);
      } else {
        return Filter.and(filters);
      }
    }
  }

  @XmlType
  @XmlEnum
  public enum SearchSelectInputMatchStyle {
    @XmlEnumValue("contains")
    CONTAINS,
    @XmlEnumValue("startsWith")
    STARTS_WITH,
    @XmlEnumValue("endsWith")
    ENDS_WITH,
    @XmlEnumValue("equals")
    EQUALS,
    @XmlEnumValue("notEquals")
    NOT_EQUALS,
    @XmlEnumValue("lessThan")
    LESS_THAN,
    @XmlEnumValue("greaterThan")
    GREATER_THAN,
    @XmlEnumValue("lessOrEqual")
    LESS_OR_EQUAL,
    @XmlEnumValue("greaterOrEqual")
    GREATER_OR_EQUALS
  }

  @XmlType
  public static class SearchSelectInput {

    @XmlAttribute private String name;

    @XmlAttribute private String field;

    @XmlAttribute private SearchSelectInputMatchStyle matchStyle;

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

    public SearchSelectInputMatchStyle getMatchStyle() {
      return matchStyle;
    }

    public void setMatchStyle(SearchSelectInputMatchStyle matchStyle) {
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
