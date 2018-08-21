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
package com.axelor.db;

import com.axelor.auth.db.AuditableModel;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.hibernate.type.JsonFunction;
import com.axelor.db.internal.DBHelper;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.axelor.rpc.Resource;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.TypedQuery;

/**
 * The {@code Query} class allows filtering and fetching records quickly.
 *
 * <p>It also provides {@link #update(Map)} and {@link #delete()} method to perform mass update and
 * delete operation on matched records.
 */
public class Query<T extends Model> {

  private Class<T> beanClass;

  private String filter;

  private Object[] params;

  private Map<String, Object> namedParams;

  private String orderBy;

  private JoinHelper joinHelper;

  private boolean cacheable;

  private boolean readOnly;

  private FlushModeType flushMode = FlushModeType.AUTO;

  private static final String NAME_PATTERN = "((?:[a-zA-Z_]\\w+)(?:(?:\\[\\])?\\.\\w+)*)";

  private static final Pattern PLACEHOLDER_PLAIN = Pattern.compile("(?<!\\?)\\?(?!(\\d+|\\?))");
  private static final Pattern PLACEHOLDER_INDEXED = Pattern.compile("\\?\\d+");

  /**
   * Create a new instance of {@code Query} with given bean class.
   *
   * @param beanClass model bean class
   */
  public Query(Class<T> beanClass) {
    this.beanClass = beanClass;
    this.orderBy = "";
    this.joinHelper = new JoinHelper(beanClass);
  }

  public static <T extends Model> Query<T> of(Class<T> klass) {
    return new Query<T>(klass);
  }

  protected EntityManager em() {
    return JPA.em();
  }

  /**
   * A convenient method to filter the query using JPQL's <i>where</i> clause.
   *
   * <p>The filter string should refer the field names with {@code self.} prefix and values should
   * not be embedded into the filter string but should be passed by parameters and {@code ?}
   * placeholder should be used to mark parameter substitutions.
   *
   * <p>Here is an example:
   *
   * <pre>
   * Query&lt;Person&gt; query = Query.of(Person);
   * query = query.filter(&quot;self.name = ? AND self.age &gt;= ?&quot;, &quot;some&quot;, 20);
   *
   * List&lt;Person&gt; matched = query.fetch();
   * </pre>
   *
   * <p>This is equivalent to:
   *
   * <pre>
   * SELECT self from Person self WHERE (self.name = ?1) AND (self.age &gt;= ?2)
   * </pre>
   *
   * <p>The params passed will be added as positional parameters to the JPA query object before
   * performing {@link #fetch()}.
   *
   * @param filter the filter string
   * @param params the parameters
   * @return the same instance
   */
  public Query<T> filter(String filter, Object... params) {
    if (this.filter != null) {
      throw new IllegalStateException("Query is already filtered.");
    }
    if (StringUtils.isBlank(filter)) {
      throw new IllegalArgumentException("filter string is required.");
    }

    // check for mixed style positional parameters
    if (PLACEHOLDER_PLAIN.matcher(filter).find() && PLACEHOLDER_INDEXED.matcher(filter).find()) {
      throw new IllegalArgumentException(
          "JDBC and JPA-style positional parameters can't be mixed: " + filter);
    }

    // fix JDBC style parameters
    int i = 1;
    final Matcher matcher = PLACEHOLDER_PLAIN.matcher(filter);
    final StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(sb, "?" + (i++));
    }
    matcher.appendTail(sb);

    this.filter = joinHelper.parse(sb.toString());
    this.params = params;
    return this;
  }

  public Query<T> filter(String filter) {
    final Object[] params = {};
    return filter(filter, params);
  }

  /**
   * Set order by clause for the query. This method can be chained to provide multiple fields.
   *
   * <p>The {@code spec} is just a field name for {@code ASC} or should be prefixed with {@code -}
   * for {@code DESC} clause.
   *
   * <p>For example:
   *
   * <pre>
   * Query&lt;Person&gt; query = Query.of(Person);
   * query = query.filter(&quot;name =&quot;, &quot;some&quot;).filter(&quot;age &gt;=&quot;, 20)
   * 		.filter(&quot;lang in&quot;, &quot;en&quot;, &quot;hi&quot;);
   *
   * query = query.order(&quot;name&quot;).order(&quot;-age&quot;);
   * </pre>
   *
   * <p>This is equivalent to:
   *
   * <pre>
   * SELECT p from Person p WHERE (p.name = ?1) AND (p.age &gt;= ?2) AND (lang IN (?3, ?4)) ORDER BY p.name, p.age DESC
   * </pre>
   *
   * @param spec order spec
   * @return the same query instance
   */
  public Query<T> order(String spec) {
    if (orderBy.length() > 0) {
      orderBy += ", ";
    } else {
      orderBy = " ORDER BY ";
    }

    String name = spec.trim();

    if (name.matches("(-)?\\s*(self\\.).*")) {
      throw new IllegalArgumentException(
          "Query#order(String) called with 'self' prefixed argument: " + spec);
    }

    if (name.charAt(0) == '-') {
      name = name.substring(1);
      orderBy += this.joinHelper.joinName(name) + " DESC";
    } else {
      orderBy += this.joinHelper.joinName(name);
    }

    return this;
  }

  /**
   * Set the query result cacheable.
   *
   * @return the same query instance
   */
  public Query<T> cacheable() {
    this.cacheable = true;
    return this;
  }

  /**
   * Set the query readonly.
   *
   * @return the same query instance.
   */
  public Query<T> readOnly() {
    this.readOnly = true;
    return this;
  }

  public Query<T> autoFlush(boolean auto) {
    this.flushMode = auto ? FlushModeType.AUTO : FlushModeType.COMMIT;
    return this;
  }

  /**
   * Fetch all the matched records as {@link Stream}.
   *
   * <p>Recommended only when dealing with large data, for example, batch processing. For normal use
   * cases, the {@link #fetch()} is more appropriate.
   *
   * <p>Also configure <code>hibernate.jdbc.fetch_size</code> (default is 20) to fine tune the fetch
   * size.
   *
   * @return stream of matched records.
   * @see #fetchSteam(int)
   * @see #fetchSteam(int, int)
   */
  public Stream<T> fetchSteam() {
    return fetchSteam(0, 0);
  }

  /**
   * Fetch the matched records as {@link Stream} with the given limit.
   *
   * <p>Recommended only when dealing with large data, for example, batch processing. For normal use
   * cases, the {@link #fetch()} is more appropriate.
   *
   * <p>Also configure <code>hibernate.jdbc.fetch_size</code> (default is 20) to fine tune the fetch
   * size.
   *
   * @param limit the limit
   * @return stream of matched records within the limit
   * @see #fetchSteam(int, int)
   */
  public Stream<T> fetchSteam(int limit) {
    return fetchSteam(limit, 0);
  }

  /**
   * Fetch the matched records as {@link Stream} within the given range.
   *
   * <p>Recommended only when dealing with large data, for example, batch processing. For normal use
   * cases, the {@link #fetch()} is more appropriate.
   *
   * <p>Also configure <code>hibernate.jdbc.fetch_size</code> (default is 20) to fine tune the fetch
   * size.
   *
   * @param limit the limit
   * @param offset the offset
   * @return stream of matched records within the range
   */
  public Stream<T> fetchSteam(int limit, int offset) {
    final org.hibernate.query.Query<T> query =
        (org.hibernate.query.Query<T>) fetchQuery(limit, offset);
    if (limit <= 0) {
      query.setFetchSize(DBHelper.getJdbcFetchSize());
    }
    return query.stream();
  }

  /**
   * Fetch all the matched records.
   *
   * @return list of all the matched records.
   */
  public List<T> fetch() {
    return fetch(0, 0);
  }

  /**
   * Fetch the matched records with the given limit.
   *
   * @param limit the limit
   * @return matched records within the limit
   */
  public List<T> fetch(int limit) {
    return fetch(limit, 0);
  }

  /**
   * Fetch the matched records within the given range.
   *
   * @param limit the limit
   * @param offset the offset
   * @return list of matched records within the range
   */
  public List<T> fetch(int limit, int offset) {
    return fetchQuery(limit, offset).getResultList();
  }

  private TypedQuery<T> fetchQuery(int limit, int offset) {
    final TypedQuery<T> query = em().createQuery(selectQuery(), beanClass);
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    if (offset > 0) {
      query.setFirstResult(offset);
    }

    final QueryBinder binder = this.bind(query).opts(cacheable, flushMode);
    if (readOnly) {
      binder.setReadOnly();
    }
    return query;
  }

  /**
   * Fetch the first matched record.
   *
   * @return the first matched record.
   */
  public T fetchOne() {
    try {
      return fetch(1).get(0);
    } catch (IndexOutOfBoundsException e) {
    }
    return null;
  }

  /**
   * Fetch a matched record at the given offset.
   *
   * @param offset the offset
   * @return the matched record at given offset
   */
  public T fetchOne(int offset) {
    try {
      return fetch(1, offset).get(0);
    } catch (IndexOutOfBoundsException e) {
    }
    return null;
  }

  /**
   * Returns the number of total records matched.
   *
   * @return total number
   */
  public long count() {
    final TypedQuery<Long> query = em().createQuery(countQuery(), Long.class);
    this.bind(query).setCacheable(cacheable).setFlushMode(flushMode).setReadOnly();
    return query.getSingleResult();
  }

  /**
   * Return a selector to select records with specific fields only.
   *
   * @param names field names to select
   * @return a new instance of {@link Selector}
   */
  public Selector select(String... names) {
    return new Selector(names);
  };

  /**
   * Perform mass update on the matched records with the given values.
   *
   * @param values the key value map
   * @return total number of records updated
   */
  public int update(Map<String, Object> values) {
    return update(values, null);
  }

  /**
   * This is similar to {@link #update(Map)} but updates only single field.
   *
   * @param name the field name whose value needs to be changed
   * @param value the new value
   * @return total number of records updated
   */
  public int update(String name, Object value) {
    Map<String, Object> values = new HashMap<>();
    values.put(name.replaceFirst("^self\\.", ""), value);
    return update(values);
  }

  /**
   * Perform mass update on matched records with the given values.
   *
   * <p>If <code>updatedBy</code> user is null, perform non-versioned update otherwise performed
   * versioned update.
   *
   * @param values the key value map
   * @param updatedBy the user to set "updatedBy" field
   * @return total number of records updated
   */
  public int update(Map<String, Object> values, User updatedBy) {
    final Map<String, Object> params = new HashMap<>();
    final Map<String, Object> namedParams = new HashMap<>();
    final List<String> where = new ArrayList<>();

    if (this.namedParams != null) {
      namedParams.putAll(this.namedParams);
    }

    for (String key : values.keySet()) {
      String name = key.replaceFirst("^self\\.", "");
      Object value = values.get(key);
      params.put(name, value);
      if (value == null) {
        where.add("self." + name + " IS NOT NULL");
      } else {
        where.add("(self." + name + " IS NULL OR " + "self." + name + " != :" + name + ")");
      }
    }

    if (updatedBy != null && AuditableModel.class.isAssignableFrom(beanClass)) {
      params.put("updatedBy", updatedBy);
      params.put("updatedOn", LocalDateTime.now());
    }

    namedParams.putAll(params);

    boolean versioned = updatedBy != null;
    boolean notMySQL = !DBHelper.isMySQL();

    String whereClause = String.join(" AND ", where);
    String selectQuery =
        selectQuery().replaceFirst("SELECT self", "SELECT self.id").replaceFirst(" ORDER BY.*", "");

    if (selectQuery.contains(" WHERE ")) {
      selectQuery = selectQuery.replaceFirst(" WHERE ", " WHERE " + whereClause + " AND ");
    } else {
      selectQuery = selectQuery + " WHERE " + whereClause;
    }

    selectQuery = selectQuery.replaceAll("\\bself", "that");

    if (notMySQL) {
      return QueryBinder.of(
              em().createQuery(updateQuery(params, versioned, "self.id IN (" + selectQuery + ")")))
          .bind(namedParams, this.params)
          .getQuery()
          .executeUpdate();
    }

    // MySQL doesn't allow sub select on same table with UPDATE also, JPQL doesn't
    // support JOIN with UPDATE query so we have to update in batch.

    String updateQuery = updateQuery(params, versioned, "self.id IN (:ids)");

    int count = 0;
    int limit = 1000;

    TypedQuery<Long> sq = em().createQuery(selectQuery, Long.class);
    javax.persistence.Query uq = em().createQuery(updateQuery);

    QueryBinder.of(sq).bind(namedParams, this.params);
    QueryBinder.of(uq).bind(namedParams, this.params);

    sq.setFirstResult(0);
    sq.setMaxResults(limit);

    List<Long> ids = sq.getResultList();
    while (!ids.isEmpty()) {
      uq.setParameter("ids", ids);
      count += uq.executeUpdate();
      ids = sq.getResultList();
    }

    return count;
  }

  /**
   * This is similar to {@link #update(Map, User)} but updates only single field.
   *
   * @param name the field name whose value needs to be changed
   * @param value the new value
   * @param updatedBy the user to set "updatedBy" field
   * @return total number of records updated
   */
  public int update(String name, Object value, User updatedBy) {
    Map<String, Object> values = new HashMap<>();
    values.put(name.replaceFirst("^self\\.", ""), value);
    return update(values, updatedBy);
  }

  /**
   * Bulk delete all the matched records. <br>
   * <br>
   * This method uses <code>DELETE</code> query and performs {@link
   * javax.persistence.Query#executeUpdate()}.
   *
   * @see #remove()
   * @return total number of records affected.
   */
  public int delete() {
    boolean notMySQL = !DBHelper.isMySQL();
    String selectQuery =
        selectQuery()
            .replaceFirst("SELECT self", "SELECT self.id")
            .replaceFirst(" ORDER BY.*", "")
            .replaceAll("\\bself", "that");

    if (notMySQL) {
      javax.persistence.Query q = em().createQuery(deleteQuery("self.id IN (" + selectQuery + ")"));
      this.bind(q);
      return q.executeUpdate();
    }

    // MySQL doesn't allow sub select on same table with DELETE also, JPQL doesn't
    // support JOIN with DELETE query so we have to update in batch.

    TypedQuery<Long> sq = em().createQuery(selectQuery, Long.class);
    javax.persistence.Query dq = em().createQuery(deleteQuery("self.id IN (:ids)"));

    this.bind(sq);
    this.bind(dq);

    int count = 0;
    int limit = 1000;

    sq.setFirstResult(0);
    sq.setMaxResults(limit);

    List<Long> ids = sq.getResultList();
    while (!ids.isEmpty()) {
      dq.setParameter("ids", ids);
      count += dq.executeUpdate();
      ids = sq.getResultList();
    }

    return count;
  }

  /**
   * Remove all the matched records. <br>
   * <br>
   * In contrast to the {@link #delete()} method, it performs {@link EntityManager#remove(Object)}
   * operation by fetching objects in pages (100 at a time).
   *
   * @see #delete()
   * @return total number of records removed.
   */
  public long remove() {
    return fetchSteam().peek(JPA::remove).count();
  }

  protected String selectQuery() {
    StringBuilder sb =
        new StringBuilder("SELECT self FROM ")
            .append(beanClass.getSimpleName())
            .append(" self")
            .append(joinHelper.toString());
    if (filter != null && filter.trim().length() > 0) sb.append(" WHERE ").append(filter);
    sb.append(orderBy);
    return sb.toString();
  }

  protected String countQuery() {
    StringBuilder sb =
        new StringBuilder("SELECT COUNT(self.id) FROM ")
            .append(beanClass.getSimpleName())
            .append(" self")
            .append(joinHelper.toString());
    if (filter != null && filter.trim().length() > 0) sb.append(" WHERE ").append(filter);
    return sb.toString();
  }

  protected String updateQuery(Map<String, Object> values, boolean versioned, String filter) {
    final String items =
        values
            .keySet()
            .stream()
            .map(key -> String.format("self.%s = :%s", key, key))
            .collect(Collectors.joining(", "));

    final StringBuilder sb =
        new StringBuilder("UPDATE ")
            .append(versioned ? "VERSIONED " : "")
            .append(beanClass.getSimpleName())
            .append(" self")
            .append(" SET ")
            .append(items);

    if (StringUtils.notBlank(filter)) {
      sb.append(" WHERE ").append(filter);
    }

    return sb.toString();
  }

  protected String deleteQuery(String filter) {
    final StringBuilder sb =
        new StringBuilder("DELETE FROM ").append(beanClass.getSimpleName()).append(" self");
    if (StringUtils.notBlank(filter)) {
      sb.append(" WHERE ").append(filter);
    }
    return sb.toString();
  }

  protected QueryBinder bind(javax.persistence.Query query) {
    return QueryBinder.of(query).bind(namedParams, params);
  }

  /**
   * Bind the named parameters of the query with the given values. Named parameter must me set after
   * query is filtered.
   *
   * @param params mapping for named params.
   * @return the same instance
   */
  public Query<T> bind(Map<String, Object> params) {
    if (this.namedParams == null) {
      this.namedParams = Maps.newHashMap();
    }
    if (params != null) {
      this.namedParams.putAll(params);
    }
    return this;
  }

  /**
   * Bind the given named parameter of the query with the given value.
   *
   * @param name the named parameter to bind
   * @param value the parameter value
   * @return the same instance
   */
  public Query<T> bind(String name, Object value) {
    Map<String, Object> params = Maps.newHashMap();
    params.put(name, value);
    return this.bind(params);
  }

  @Override
  public String toString() {
    return selectQuery();
  }

  /**
   * A helper class to select specific field values. The record is returned as a Map object with the
   * given names as keys.
   *
   * <pre>
   * List&lt;Map&gt; data = Contact.all().filter(&quot;self.age &gt; ?&quot;, 20)
   * 		.select(&quot;title.name&quot;, &quot;fullName&quot;, &quot;age&quot;).fetch(80, 0);
   * </pre>
   *
   * This results in following query:
   *
   * <pre>
   * SELECT _title.name, self.fullName JOIN LEFT self.title AS _title WHERE self.age &gt; ? LIMIT 80
   * </pre>
   *
   * The {@link Selector#fetch(int, int)} method returns a List of Map instead of the model object.
   */
  public class Selector {

    private List<String> names = Lists.newArrayList("id", "version");
    private List<String> collections = Lists.newArrayList();
    private String query;
    private Mapper mapper = Mapper.of(beanClass);

    private Selector(String... names) {
      List<String> selects = Lists.newArrayList();
      selects.add("self.id");
      selects.add("self.version");
      for (String name : names) {
        Property property = getProperty(name);
        if (property != null
            && property.getType() != PropertyType.BINARY
            && !property.isTransient()) {
          String alias = joinHelper.joinName(name);
          if (alias != null) {
            selects.add(alias);
            this.names.add(name);
          } else {
            collections.add(name);
          }
          // select id,version,name field for m2o
          if (property.isReference() && property.getTargetName() != null) {
            this.names.add(name + ".id");
            this.names.add(name + ".version");
            this.names.add(name + "." + property.getTargetName());
            selects.add(joinHelper.joinName(name + ".id"));
            selects.add(joinHelper.joinName(name + ".version"));
            selects.add(joinHelper.joinName(name + "." + property.getTargetName()));
          }
        } else if (name.indexOf('.') > -1) {
          final JsonFunction func = JsonFunction.fromPath(name);
          final Property json = mapper.getProperty(func.getField());
          if (json != null && json.isJson()) {
            this.names.add(func.getField() + "." + func.getAttribute());
            selects.add(func.toString());
          }
        }
      }

      StringBuilder sb =
          new StringBuilder("SELECT ")
              .append(" new List(" + Joiner.on(", ").join(selects) + ")")
              .append(" FROM ")
              .append(beanClass.getSimpleName())
              .append(" self")
              .append(joinHelper.toString());
      if (filter != null && filter.trim().length() > 0) sb.append(" WHERE ").append(filter);
      sb.append(orderBy);
      query = sb.toString();
    }

    private Property getProperty(String field) {
      if (field == null || "".equals(field.trim())) return null;
      Mapper mapper = this.mapper;
      Property property = null;
      Iterator<String> names = Splitter.on(".").split(field).iterator();
      while (names.hasNext()) {
        property = mapper.getProperty(names.next());
        if (property == null) return null;
        if (names.hasNext()) {
          if (property.getTarget() == null) return null;
          mapper = Mapper.of(property.getTarget());
        }
      }
      return property;
    }

    @SuppressWarnings("all")
    public List<List> values(int limit, int offset) {
      javax.persistence.Query q = em().createQuery(query);
      if (limit > 0) {
        q.setMaxResults(limit);
      }
      if (offset > 0) {
        q.setFirstResult(offset);
      }

      final QueryBinder binder = bind(q).opts(cacheable, flushMode);
      if (readOnly) {
        binder.setReadOnly();
      }

      return q.getResultList();
    }

    @SuppressWarnings("all")
    public List<Map> fetch(int limit, int offset) {

      List<List> data = values(limit, offset);
      List<Map> result = Lists.newArrayList();

      for (List items : data) {
        Map<String, Object> map = Maps.newHashMap();
        for (int i = 0; i < names.size(); i++) {
          Object value = items.get(i);
          String name = names.get(i);
          Property property = getProperty(name);
          // in case of m2o, get the id,version,name tuple
          if (property != null && property.isReference() && property.getTargetName() != null) {
            value = getReferenceValue(items, i);
            i += 3;
          } else if (value instanceof Model) {
            value = Resource.toMapCompact(value);
          }
          map.put(name, value);
        }
        if (collections.size() > 0) {
          map.putAll(this.fetchCollections(items.get(0)));
        }
        result.add(map);
      }

      return result;
    }

    private Object getReferenceValue(List<?> items, int at) {
      if (items.get(at) == null) {
        return null;
      }
      Map<String, Object> value = Maps.newHashMap();
      String name = names.get(at);
      String nameField = names.get(at + 3).replace(name + ".", "");

      value.put("id", items.get(at + 1));
      value.put("$version", items.get(at + 2));
      value.put(nameField, items.get(at + 3));

      return value;
    }

    @SuppressWarnings("all")
    private Map<String, List> fetchCollections(Object id) {
      Map<String, List> result = Maps.newHashMap();
      Object self = JPA.em().find(beanClass, id);
      for (String name : collections) {
        Collection<Model> items = (Collection<Model>) mapper.get(self, name);
        if (items != null) {
          List<Object> all = Lists.newArrayList();
          for (Model obj : items) {
            all.add(Resource.toMapCompact(obj));
          }
          result.put(name, all);
        }
      }
      return result;
    }

    @Override
    public String toString() {
      return query;
    }
  }

  /**
   * JoinHelper class is used to auto generate <code>LEFT JOIN</code> for association expressions.
   *
   * <p>For example:
   *
   * <pre>
   * 	Query<Contact> q = Contact.all().filter("self.title.code = ?1 OR self.age > ?2", "mr", 20);
   * </pre>
   *
   * Results in:
   *
   * <pre>
   * SELECT self FROM Contact self LEFT JOIN self.title _title WHERE _title.code = ?1 OR self.age > ?2
   * </pre>
   *
   * So that all the records are matched even if <code>title</code> field is null.
   */
  static class JoinHelper {

    private Class<?> beanClass;

    private Map<String, String> joins = Maps.newLinkedHashMap();

    private static final Pattern pathPattern = Pattern.compile("self\\." + NAME_PATTERN);

    public JoinHelper(Class<?> beanClass) {
      this.beanClass = beanClass;
    }

    /**
     * Parse the given filter string and return transformed filter expression.
     *
     * <p>Automatically calculate <code>LEFT JOIN</code> for association path expressions and the
     * path expressions are replaced with the join variables.
     *
     * @param filter the filter expression
     * @return the transformed filter expression
     */
    public String parse(String filter) {

      String result = "";
      Matcher matcher = pathPattern.matcher(filter);

      int last = 0;
      while (matcher.find()) {
        MatchResult matchResult = matcher.toMatchResult();
        String alias = joinName(matchResult.group(1));
        if (alias == null) {
          alias = "self." + matchResult.group(1);
        }
        result += filter.substring(last, matchResult.start()) + alias;
        last = matchResult.end();
      }
      if (last < filter.length()) result += filter.substring(last);

      return result;
    }

    /**
     * Automatically generate <code>LEFT JOIN</code> for the given name (association path
     * expression) and return the join variable.
     *
     * @param name the path expression or field name
     * @return join variable if join is created else returns name
     */
    public String joinName(String name) {

      Mapper mapper = Mapper.of(beanClass);
      String[] path = name.split("\\.");
      String prefix = null;
      String variable = name;

      if (path.length > 1) {
        variable = path[path.length - 1];
        String joinOn = null;
        Mapper currentMapper = mapper;
        for (int i = 0; i < path.length - 1; i++) {
          String item = path[i].replace("[]", "");
          Property property = currentMapper.getProperty(item);
          if (property == null) {
            throw new org.hibernate.QueryException(
                "could not resolve property: "
                    + item
                    + " of: "
                    + currentMapper.getBeanClass().getName());
          }

          if (property.isJson()) {
            return JsonFunction.fromPath(name).toString();
          }

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

          if (property.getTarget() != null) {
            currentMapper = Mapper.of(property.getTarget());
          }

          if (i == path.length - 2) {
            property = currentMapper.getProperty(variable);
            if (property == null) {
              throw new IllegalArgumentException(
                  String.format(
                      "No such field '%s' in object '%s'",
                      variable, currentMapper.getBeanClass().getName()));
            }
            if (property != null && property.getTarget() != null) {
              joinOn = prefix + "." + variable;
              prefix = prefix + "_" + variable;
              joins.put(joinOn, prefix);
              return prefix;
            }
          }
        }
      } else {
        Property property = mapper.getProperty(name);
        if (property != null && property.getTarget() != null) {
          if (property.isCollection()) {
            return null;
          }
          prefix = "_" + name;
          joins.put("self." + name, prefix);
          return prefix;
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
      for (String key : joins.keySet()) {
        String val = joins.get(key);
        joinItems.add("LEFT JOIN " + key + " " + val);
      }
      return " " + Joiner.on(" ").join(joinItems);
    }
  }
}
