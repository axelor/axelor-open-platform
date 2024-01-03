/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.db;

import com.axelor.db.mapper.Adapter;
import com.axelor.db.mapper.Mapper;
import com.axelor.rpc.ContextEntity;
import com.axelor.script.ScriptBindings;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.FlushModeType;
import javax.persistence.Parameter;
import org.hibernate.jpa.QueryHints;

/**
 * The query binder class provides the helper methods to bind query parameters and mark the query
 * cacheable.
 */
public class QueryBinder {

  private final javax.persistence.Query query;

  /**
   * Create a new query binder for the given query instance.
   *
   * @param query the query instance
   */
  private QueryBinder(javax.persistence.Query query) {
    this.query = query;
  }

  /**
   * Create a new query binder for the given query instance.
   *
   * @param query the query instance
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
   * @param cacheable whether to set cacheable or not
   * @return the same query binder instance
   */
  public QueryBinder setCacheable(boolean cacheable) {
    query.setHint(QueryHints.HINT_CACHEABLE, cacheable);
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
   * <p>This will give better performance if the result is not meant for updates. For example, REST
   * api data fetching can benefit from this.
   *
   * @return the same query binder instance
   */
  public QueryBinder setReadOnly(boolean readOnly) {
    query.setHint(QueryHints.HINT_READONLY, readOnly);
    return this;
  }

  /**
   * Set query flush mode.
   *
   * @param mode flush mode
   * @return the same query binder instance
   */
  public QueryBinder setFlushMode(FlushModeType mode) {
    query.setFlushMode(mode);
    return this;
  }

  /**
   * Shortcut to the {@link #setCacheable()} and {@link #setFlushMode(FlushModeType)} methods.
   *
   * @param cacheable whether to mark the query cacheable
   * @param type the {@link FlushModeType}, only set if type is not null
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
   * <p>The parameter values will be automatically adapted to correct data type of the query
   * parameter.
   *
   * @param namedParams the named parameters
   * @param params the positional parameters
   * @return the same query binder instance
   */
  public QueryBinder bind(Map<String, Object> namedParams, Object... params) {

    final ScriptBindings bindings;

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
          final String name = p.getName();
          Object value = bindings.get(name);
          if (value == null && name.indexOf('$', 1) >= 0) {
            final String dottedName = name.replaceAll("(?<=\\w)\\$", ".");
            value = getDottedValue(bindings, dottedName);
          }
          this.bind(name, value);
        }
      }
    }

    if (params == null) {
      return this;
    }

    for (int i = 0; i < params.length; i++) {
      int pos = i + 1;
      Parameter<?> param;
      Object value = params[i];
      if (value instanceof ContextEntity) {
        value = ((ContextEntity) value).getContextId();
      } else if (value instanceof Model) {
        value = ((Model) value).getId();
      } else if (value instanceof String) {
        final String expr = (String) value;
        if (expr.startsWith("__") && expr.endsWith("__") && bindings.containsKey(expr)) {
          // special variable
          value = bindings.get(expr);
        }
      }
      try {
        param = query.getParameter(pos);
      } catch (Exception e) {
        continue;
      }
      try {
        query.setParameter(pos, value);
      } catch (IllegalArgumentException e) {
        query.setParameter(pos, adapt(value, param));
      }
    }

    return this;
  }

  private Object getDottedValue(ScriptBindings bindings, String dottedName) {
    final Object value = bindings.get(dottedName);

    if (value != null) {
      return value;
    }

    final List<String> names = Splitter.on('.').splitToList(dottedName);
    return getRelatedValue(bindings.get(names.get(0)), names.subList(1, names.size()));
  }

  private Object getRelatedValue(Object value, Collection<String> subNames) {
    for (final String name : subNames) {
      if (value == null) {
        break;
      } else if (value instanceof Map) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) value;
        value = map.get(name);
      } else if (value instanceof Collection) {
        @SuppressWarnings("unchecked")
        final Collection<Object> items = (Collection<Object>) value;
        value =
            items.stream()
                .map(item -> getRelatedValue(item, Collections.singleton(name)))
                .collect(Collectors.toList());
      } else {
        value = Mapper.of(value.getClass()).get(value, name);
      }
    }
    return value;
  }

  /**
   * Bind the given named parameter with the given value.
   *
   * @param name the named parameter
   * @param value the parameter value
   * @return the same query binder instance
   */
  public QueryBinder bind(String name, Object value) {
    Parameter<?> parameter = null;
    try {
      parameter = query.getParameter(name);
    } catch (Exception e) {
    }

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
