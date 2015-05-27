/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.TypedQuery;

import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.rpc.Resource;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Provider;

/**
 * The {@code Query} class allows filtering and fetching records quickly.
 *
 * <p>
 * It also provides {@link #update(Map)} and {@link #delete()} method to perform
 * mass update and delete operation on matched records.
 *
 */
public class Query<T extends Model> {

	private Class<T> beanClass;

	private String filter;

	private Object[] params;

	private Map<String, Object> namedParams;

	private String orderBy;

	private List<String> orderByNames;

	private JoinHelper joinHelper;

	private boolean cacheable;

	private FlushModeType flushMode = FlushModeType.AUTO;

	private static final String NAME_PATTERN = "((?:[a-zA-Z_]\\w+)(?:(?:\\[\\])?\\.\\w+)*)";

	private static final Pattern orderPattern = Pattern.compile(
			"(\\-)?(?:self\\.)?" + NAME_PATTERN, Pattern.CASE_INSENSITIVE);

	/**
	 * Create a new instance of {@code Query} with given bean class.
	 *
	 * <p>
	 * Before using the instance, an {@code EntityManager} provider should be
	 * either injected by the {@code Guice} container or should be provided
	 * manually using {@link #setEntityManagerProvider(Provider)} method.
	 *
	 * @param beanClass
	 *            model bean class
	 */
	public Query(Class<T> beanClass) {
		this.beanClass = beanClass;
		this.orderBy = "";
		this.orderByNames = new ArrayList<>();
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
	 * <p>
	 * The filter string should refer the field names with {@code self.} prefix
	 * and values should not be embedded into the filter string but should be
	 * passed by parameters and {@code ?} placeholder should be used to mark
	 * parameter substitutions.
	 *
	 * Here is an example:
	 *
	 * <pre>
	 * Query&lt;Person&gt; query = Query.of(Person);
	 * query = query.filter(&quot;self.name = ? AND self.age &gt;= ?&quot;, &quot;some&quot;, 20);
	 *
	 * List&lt;Person&gt; matched = query.fetch();
	 * </pre>
	 *
	 * <p>
	 * This is equivalent to:
	 *
	 * <pre>
	 * SELECT self from Person self WHERE (self.name = ?1) AND (self.age >= ?2)
	 * </pre>
	 *
	 * <p>
	 * The params passed will be added as positional parameters to the JPA query
	 * object before performing {@link #fetch()}.
	 *
	 * @param filter
	 *            the filter string
	 * @param params
	 *            the parameters
	 * @return the same instance
	 */
	public Query<T> filter(String filter, Object... params) {
		if (this.filter != null) {
			throw new IllegalStateException("Query is already filtered.");
		}

		this.filter = joinHelper.parse(filter);
		this.params = params;
		return this;
	}

	public Query<T> filter(String filter) {
		final Object[] params = {};
		return filter(filter, params);
	}

	/**
	 * Set order by clause for the query. This method can be chained to provide
	 * multiple fields.
	 *
	 * <p>
	 * The {@code spec} is just a field name for {@code ASC} or should be
	 * prefixed with {@code -} for {@code DESC} clause.
	 *
	 * <p>
	 * For example:
	 *
	 * <pre>
	 * Query&lt;Person&gt; query = Query.of(Person);
	 * query = query.filter(&quot;name =&quot;, &quot;some&quot;).filter(&quot;age &gt;=&quot;, 20)
	 * 		.filter(&quot;lang in&quot;, &quot;en&quot;, &quot;hi&quot;);
	 *
	 * query = query.order(&quot;name&quot;).order(&quot;-age&quot;);
	 * </pre>
	 *
	 * <p>
	 * This is equivalent to:
	 *
	 * <pre>
	 * SELECT p from Person p WHERE (p.name = ?1) AND (p.age >= ?2) AND (lang IN (?3, ?4)) ORDER BY p.name, p.age DESC
	 * </pre>
	 *
	 * @param spec
	 *            order spec
	 * @return the same query instance
	 */
	public Query<T> order(String spec) {
		Matcher m = orderPattern.matcher(spec.trim());
		if (!m.matches()) {
			throw new IllegalArgumentException("Invalid order spec: " + spec);
		}

		if (orderBy.length() > 0) {
			orderBy += ", ";
		} else {
			orderBy = " ORDER BY ";
		}

		String name = this.joinHelper.joinName(m.group(2));

		orderByNames.add(name);
		orderBy += name + ("-".equals(m.group(1)) ? " DESC" : "");

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

	public Query<T> autoFlush(boolean auto) {
		this.flushMode = auto ? FlushModeType.AUTO : FlushModeType.COMMIT;
		return this;
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
	 * Fetch the matchied records with the given limit.
	 *
	 * @param limit
	 *            the limit
	 * @return matched records withing the limit
	 */
	public List<T> fetch(int limit) {
		return fetch(limit, 0);
	}

	/**
	 * Fetch the matched records within the given range.
	 *
	 * @param limit
	 *            the limit
	 * @param offset
	 *            the offset
	 * @return list of matched records within the range
	 */
	public List<T> fetch(int limit, int offset) {
		final TypedQuery<T> query = em().createQuery(selectQuery(), beanClass);
		if (limit > 0) {
			query.setMaxResults(limit);
		}
		if (offset > 0) {
			query.setFirstResult(offset);
		}

		this.bind(query).opts(cacheable, flushMode);
		return query.getResultList();
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
	 * @param offset
	 *            the offset
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
		this.bind(query).opts(cacheable, flushMode);
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
	 * To avoid unexpected results, clear the session with {@link JPA.clear}
	 * before running the update.
	 *
	 * @param values
	 *            the key value map
	 * @return total number of records updated
	 */
	public int update(Map<String, Object> values) {
		final Map<String, Object> params = Maps.newHashMap();
		final Map<String, Object> namedParams = Maps.newHashMap();
		
		if (this.namedParams != null) {
			namedParams.putAll(this.namedParams);
		}

		for(String key : values.keySet()) {
			String name = key.replaceFirst("^self\\.", "");
			params.put(name, values.get(key));
			namedParams.put(name, values.get(key));
		}

		javax.persistence.Query q = em().createQuery(updateQuery(params));
		QueryBinder.of(q).bind(namedParams, this.params);

		return q.executeUpdate();
	}

	/**
	 * This is similar to {@link #update(Map)} but updates only single field.
	 *
	 * @param field
	 *            the field name whose value needs to be changed
	 * @param value
	 *            the new value
	 * @return total number of records updated
	 */
	public int update(String name, Object value) {
		Map<String, Object> values = Maps.newHashMap();
		values.put(name.replaceFirst("^self\\.", ""), value);
		return update(values);
	}

	/**
	 * Bulk delete all the matched records. <br>
	 * <br>
	 *
	 * This method uses <code>DELETE</code> query and performs
	 * {@link javax.persistence.Query#executeUpdate()}.
	 *
	 * @see #remove()
	 * @return total number of records affected.
	 */
	public int delete() {
		javax.persistence.Query q = em().createQuery(deleteQuery());
		this.bind(q);
		return q.executeUpdate();
	}

	/**
	 * Remove all the matched records. <br>
	 * <br>
	 *
	 * In contrast to the {@link #delete()} method, it performs
	 * {@link EntityManager#remove(Object)} operation by fetching objects in
	 * pages (100 at a time).
	 *
	 * @see #delete()
	 * @return total number of records removed.
	 */
	public long remove() {
		long n = this.count();
		while(this.count() > 0) {
			for(T o : this.fetch(100))
				JPA.remove(o);
		}
		return n;
	}

	protected String selectQuery() {
		StringBuilder sb = new StringBuilder("SELECT self FROM ")
			.append(beanClass.getSimpleName())
			.append(" self")
			.append(joinHelper.toString());
		if (filter != null && filter.trim().length() > 0)
			sb.append(" WHERE ").append(filter);
		sb.append(orderBy);
		return sb.toString();
	}

	protected String countQuery() {
		StringBuilder sb = new StringBuilder("SELECT COUNT(self) FROM ")
				.append(beanClass.getSimpleName())
				.append(" self")
				.append(joinHelper.toString());
		if (filter != null && filter.trim().length() > 0)
			sb.append(" WHERE ").append(filter);
		return sb.toString();
	}

	protected String updateQuery(Map<String, Object> values) {
		StringBuilder sb = new StringBuilder("UPDATE ")
				.append(beanClass.getSimpleName())
				.append(" self");
		List<String> keys = Lists.newArrayList();
		for (String key : values.keySet()) {
			keys.add(String.format("self.%s = :%s", key, key));
		}
		sb.append(" SET ").append(Joiner.on(", ").join(keys));
		if (filter != null && filter.trim().length() > 0) {
			sb.append(" WHERE self.id IN (")
			  .append(selectQuery().replaceFirst("SELECT self", "SELECT self.id").replaceAll("\\bself", "that"))
			  .append(")");
		}
		return sb.toString();
	}

	protected String deleteQuery() {
		StringBuilder sb = new StringBuilder("DELETE FROM ")
				.append(beanClass.getSimpleName())
				.append(" self");
		if (filter != null && filter.trim().length() > 0) {
			sb.append(" WHERE self.id IN (")
			  .append(selectQuery().replaceFirst("SELECT self", "SELECT self.id").replaceAll("\\bself", "that"))
			  .append(")");
		}
		return sb.toString();
	}

	protected QueryBinder bind(javax.persistence.Query query) {
		return QueryBinder.of(query).bind(namedParams, params);
	}

	/**
	 * Bind the named parameters of the query with the given values. Named
	 * parameter must me set after query is filtered.
	 *
	 * @param params
	 *            mapping for named params.
	 */
	public Query<T> bind(Map<String, Object> params) {
		if (this.filter == null) {
			throw new IllegalStateException("Query is not filtered yet.");
		}
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
	 * @param name
	 *            the named parameter to bind
	 * @param value
	 *            the parameter value
	 *
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
	 * A helper class to select specific field values. The record is returned as
	 * a Map object with the given names as keys.
	 *
	 * <pre>
	 * List&lt;Map&gt; data = Contact.all().filter(&quot;self.age &gt; ?&quot;, 20)
	 * 		.select(&quot;title.name&quot;, &quot;fullName&quot;, &quot;age&quot;).fetch(80, 0);
	 * </pre>
	 *
	 * This results in following query:
	 *
	 * <pre>
	 * SELECT _title.name, self.fullName JOIN LEFT self.title AS _title WHERE self.age > ? LIMIT 80
	 * </pre>
	 *
	 * The {@link Selector#fetch(int, int)} method returns a List of Map instead
	 * of the model object.
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
			for(String name : names) {
				Property property = getProperty(name);
				if (property != null) {
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
				}
			}

			// order by names are required for SELECT DISTINCT
			for (String name : orderByNames) {
				if (!selects.contains(name)) {
					selects.add(name);
					this.names.add(name);
				}
			}

			StringBuilder sb = new StringBuilder("SELECT DISTINCT")
				.append(" new List(" + Joiner.on(", ").join(selects) + ")")
				.append(" FROM ")
				.append(beanClass.getSimpleName())
				.append(" self")
				.append(joinHelper.toString());
			if (filter != null && filter.trim().length() > 0)
				sb.append(" WHERE ").append(filter);
			sb.append(orderBy);
			query = sb.toString();
		}

		private Property getProperty(String field) {
			if (field == null || "".equals(field.trim()))
				return null;
			Mapper mapper = this.mapper;
			Property property = null;
			Iterator<String> names = Splitter.on(".").split(field).iterator();
			while(names.hasNext()) {
				property = mapper.getProperty(names.next());
				if (property == null)
					return null;
				if (names.hasNext()) {
					if (property.getTarget() == null)
						return null;
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

			bind(q).opts(cacheable, flushMode);

			return q.getResultList();
		}

		@SuppressWarnings("all")
		public List<Map> fetch(int limit, int offset) {

			List<List> data = values(limit, offset);
			List<Map> result = Lists.newArrayList();

			for(List items : data) {
				Map<String, Object> map = Maps.newHashMap();
				for(int i = 0 ; i < names.size() ; i++) {
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

			value.put("id", items.get(at+1));
			value.put("$version", items.get(at+2));
			value.put(nameField, items.get(at+3));

			return value;
		}

		@SuppressWarnings("all")
		private Map<String, List> fetchCollections(Object id) {
			Map<String, List> result = Maps.newHashMap();
			Object self = JPA.em().find(beanClass, id);
			for(String name : collections) {
				Collection<Model> items = (Collection<Model>) mapper.get(self, name);
				if (items != null) {
					List<Object> all = Lists.newArrayList();
					for(Model obj : items) {
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
	 * JoinHelper class is used to auto generate <code>LEFT JOIN</code> for
	 * association expressions.
	 *
	 * For example:
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
	 *
	 */
	static class JoinHelper {

		private Class<?> beanClass;

		private Map<String, String> joins = Maps.newLinkedHashMap();

		private static final Pattern pathPattern = Pattern.compile("self\\." + NAME_PATTERN);

		public JoinHelper(Class<?> beanClass) {
			this.beanClass = beanClass;
		}

		/**
		 * Parse the given filter string and return transformed filter
		 * expression.
		 *
		 * Automatically calculate <code>LEFT JOIN</code> for association path
		 * expressions and the path expressions are replaced with the join
		 * variables.
		 *
		 * @param filter
		 *            the filter expression
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
			if (last < filter.length())
		        result += filter.substring(last);

			return result;
		}

		/**
		 * Automatically generate <code>LEFT JOIN</code> for the given name
		 * (association path expression) and return the join variable.
		 *
		 * @param name
		 *            the path expression or field name
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
				for(int i = 0 ; i < path.length - 1 ; i++) {
					String item = path[i].replace("[]", "");
					Property property = currentMapper.getProperty(item);
					if (property == null) {
						break;
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
									String.format("No such field '%s' in object '%s'",
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
			for(String key : joins.keySet()) {
				String val = joins.get(key);
				joinItems.add("LEFT JOIN " + key + " " + val);
			}
			return " " + Joiner.on(" ").join(joinItems);
		}
	}
}
