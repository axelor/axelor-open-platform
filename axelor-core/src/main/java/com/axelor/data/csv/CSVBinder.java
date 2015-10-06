/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
package com.axelor.data.csv;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.data.AuditHelper;
import com.axelor.data.adapter.DataAdapter;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CSVBinder {

	private static final Logger LOG = LoggerFactory.getLogger(CSVBinder.class);

	private Class<?> beanClass;

	private List<CSVBind> bindings;

	private String[] fields;

	private String query;

	private boolean update;

	private boolean newBean;

	private Map<String, DataAdapter> adapters = Maps.newHashMap();

	public void registerAdapter(DataAdapter adapter) {
		adapters.put(adapter.getName(), adapter);
	}

	public String[] getFields() {
		return fields;
	}

	public CSVBinder(Class<?> beanClass, String[] fields, CSVInput csvInput) {
		this(beanClass, fields, csvInput.getBindings(), true, csvInput.getSearch(), csvInput.isUpdate());
	}

	public CSVBinder(Class<?> beanClass, String[] fields, CSVBind csvBind) {
		this(beanClass, fields, csvBind.getBindings(), false, csvBind.getSearch(), csvBind.isUpdate());
	}

	private CSVBinder(Class<?> beanClass, String[] fields, List<CSVBind> csvBinds, boolean autoBind, String query, boolean update) {
		this.beanClass = beanClass;
		this.fields = fields;
		this.bindings = Lists.newArrayList();
		this.query = query;
		this.update = update;

		if (csvBinds != null)
			this.bindings.addAll(csvBinds);

		if (autoBind)
			this.autoBind(fields);
	}

	private void autoBind(String[] fields) {

		Set<String> beanFields = Sets.newHashSet();
		Map<String, Set<String>> refFields = Maps.newHashMap();
		List<String> boundCols = getBoundCols(this.bindings, null);

		for (String field : fields) {

			if (boundCols.contains(field))
				continue;

			if (field.contains(".")) {

				String[] parts = field.split("\\.");
				beanFields.add(parts[0]);

				Set<String> refs = refFields.get(parts[0]);
				if (refs == null) {
					refs = new HashSet<String>();
					refFields.put(parts[0], refs);
				}
				refs.add(parts[1]);

			} else {
				beanFields.add(field);
			}
		}

		for (String field : beanFields) {
			bindings.add(CSVBind.getBinding(null, field, refFields.get(field)));
		}
	}

	private List<String> getBoundCols(List<CSVBind> bindings, List<String> bounds) {
		if (bounds == null)
			bounds = Lists.newArrayList();
		if (bindings != null) {
			for (CSVBind cb : bindings) {
				if (cb.getColumn() != null)
					bounds.add(cb.getColumn());
				if (cb.getBindings() != null)
					bounds.addAll(getBoundCols(cb.getBindings(), bounds));
			}
		}
		return bounds;
	}

	@SuppressWarnings("unchecked")
	private Object find(Map<String, Object> params) {

		if (this.query != null) {
			LOG.trace("search: " + this.query);
			Object bean = JPA.all((Class<Model>) beanClass).filter(query).bind(params).cacheable().autoFlush(false).fetchOne();
			LOG.trace("search found: " + bean);
			if (update || bean != null) {
				newBean = false;
				return bean;
			}
		}

		try {
			newBean = true;
			return beanClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private Object findAll(Class<?> beanClass, String query, Map<String, Object> params) {
		return JPA.all((Class<Model>) beanClass).filter(query).bind(params).cacheable().autoFlush(false).fetch();
	}

	private boolean isBound(CSVBind cb, Map<String, Object> values) {
		if (cb.getColumn() != null)
			return values.get(cb.getColumn()) != null;
		if (cb.getBindings() != null)
			for (CSVBind b : cb.getBindings())
				if (isBound(b, values))
					return true;
		if(cb.getSearch() != null)
			return true;
		return cb.getExpression() != null;
	}

	private void handleDummyBind(CSVBind cb, Map<String, Object> values) {

		Class<?> type = null;
		try {
			type = Class.forName(cb.getType());
		} catch (ClassNotFoundException e) {
		} catch (Exception e) {
		}

		String field = cb.getField();
		Object value = null;

		if (type == null) {
			value = values.get(cb.getColumn());
			if (cb.getColumn() == null &&
				cb.getSearch() == null &&
				cb.getExpression() != null) {
				value = cb.evaluate(values);
			}
		} else {
			CSVBinder binder = new CSVBinder(type, fields, cb);
			value = binder.bind(values);
		}
		values.put(field, value);
	}

	@SuppressWarnings("all")
	private Object bind(Map<String, Object> values) {

		Mapper mapper = Mapper.of(beanClass);
		Object bean = find(values);

		if (bean == null)
			return null;

		LOG.trace("populate: " + beanClass);

		for (CSVBind cb : this.bindings) {

			LOG.trace("binding: " + cb);

			String field = cb.getField();
			Property p = mapper.getProperty(field);

			if (p == null) { // handle dummy binding
				this.handleDummyBind(cb, values);
				continue;
			}

			if (p.isPrimary() || p.isVirtual() || !isBound(cb, values)) {
				continue;
			}

			Object value = values.get(cb.getColumn());

			LOG.trace("value: " + value);
			LOG.trace("condition: " + cb.getCondition());

			if (newBean == false && cb.getConditionEmpty() == Boolean.TRUE) {
				Object o = p.get(bean);
				if(o != null && p.isCollection()) {
					if(o instanceof Collection<?> && !((Collection<?>)o).isEmpty()) {
						LOG.trace("field is not empty");
						continue;
					}
				}
				else if(o != null) {
					LOG.trace("field is not empty");
					continue;
				}
			}

			if (!cb.validate(values)) {
				LOG.trace("condition failed");
				continue;
			}

			value = this.adapt(cb, value, values);

			// get default value
			if (cb.getColumn() == null && cb.getSearch() == null && cb.getExpression() != null) {
				LOG.trace("expression: " + cb.getExpression());
				value = cb.evaluate(values);
				LOG.trace("value: " + value);
			}

			// find m2m references
			else if (p.getType() == PropertyType.MANY_TO_MANY
					&& cb.getColumn() != null && cb.getSearch() != null) {
				value = findAll(p.getTarget(), cb.getSearch(), values);
			}

			// handle relational fields (including other case of m2m)
			else if (p.getTarget() != null) {
				CSVBinder b = new CSVBinder(p.getTarget(), fields, cb);
				value = b.bind(values);
			}

			if (p.isCollection()) {
				if (value instanceof Collection<?>)
					p.addAll(bean, (Collection<?>) value);
				else
					p.add(bean, value);
			} else if (!AuditHelper.update(bean, field, value)) {
				p.set(bean, value);
			}

			if(value == null && (p.isReference() || p.isCollection()) && isValueGiven(cb, values)) {
				LOG.warn("Bind null value to {} with context: {}", p.getName(), Arrays.asList(values));
			}
			else {
				LOG.trace("set value: {} = {}", p.getName(), value);
			}
		}

		return bean;
	}
	
	private boolean isValueGiven(CSVBind bind, Map<String, Object> values) {
		if (bind.getColumn() != null) return false;
		if (bind.getBindings() == null) return true;
		boolean given = false;
		for (CSVBind binding : bind.getBindings()) {
			Object raw = values.get(binding.getColumn());
			if (raw != null && !"".equals(raw)) {
				given = true;
				break;
			}
		}
		return given;
	}

	/**
	 * Get a bean instance with the given set of <code>values</code> binding to
	 * the instance according to the binding rules.<br>
	 *
	 * <p>
	 * The <code>localContext</code> is a copy of the global context created
	 * with <code>prepare-context</code> method. The <code>localContext</code>
	 * is updated with the current binding values and is available to the
	 * <code>call</code> method as context.
	 *
	 * @param values
	 *            values from csv row
	 * @param localContext
	 *            copy of global context, the method updates it with the binding
	 *            values
	 * @return a bean instance of the class on which binding is performed
	 */
	public Object bind(String[] values, Map<String, Object> localContext) {

		Preconditions.checkNotNull(values);
		Preconditions.checkNotNull(localContext);
		Preconditions.checkArgument(values.length == fields.length);

		Map<String, Object> map = Maps.newHashMap(localContext);
		for (int i = 0; i < fields.length; i++) {
			map.put(fields[i], values[i]);
		}

		localContext.putAll(map);
		for (CSVBind cb : flatten(this.bindings)) {
			String field = cb.getColumn();
			if (Strings.isNullOrEmpty(field) || !map.containsKey(field))
				continue;
			localContext.put(field, cb.evaluate(map));
			if (field.contains("."))
				localContext.put(field.replace(".", "_") + "_", localContext.get(field));
		}

		return bind(localContext);
	}

	private List<CSVBind> flatten(List<CSVBind> bindings) {
		List<CSVBind> all = Lists.newArrayList();
		for (CSVBind cb : bindings) {
			all.add(cb);
			if (cb.getBindings() != null) {
				all.addAll(flatten(cb.getBindings()));
			}
		}
		return all;
	}

	private Object adapt(CSVBind bind, Object value, Map<String, Object> ctx) {
		String name = bind.getAdapter();
		if ("".equals(value)) {
			value = null;
		}
		if (name == null || !adapters.containsKey(name)) {
			return value;
		}
		DataAdapter adapter = adapters.get(name);
		return adapter.adapt(value, ctx);
	}
}
