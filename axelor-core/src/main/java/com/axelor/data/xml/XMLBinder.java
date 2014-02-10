/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.data.xml;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.jxpath.JXPathContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.axelor.data.adapter.DataAdapter;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.beust.jcommander.internal.Maps;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public abstract class XMLBinder {

	private static final Logger LOG = LoggerFactory.getLogger(XMLBinder.class);

	private XMLInput input;

	private Map<String, Object> context;

	private boolean newBean;

	private Map<String, DataAdapter> adapters = Maps.newHashMap();

	public XMLBinder(XMLInput input, Map<String, Object> context) {
		this.input = input;
		this.context = context;
	}

	public void registerAdapter(DataAdapter adapter) {
		adapters.put(adapter.getName(), adapter);
	}

	protected abstract void handle(Object bean, XMLBind bind, Map<String, Object> context);
	protected abstract void finish();

	private Class<?> lastClass = null;

	public void bind(Document element) {
		for(XMLBind binding : input.getBindings()) {
			LOG.debug("binding: " + binding);
			List<Node> nodes = this.find(element, binding, "/");
			for(Node node : nodes) {
				if (lastClass != binding.getType()) {
					lastClass = binding.getType();
					JPA.flush();
				}
				LOG.trace("element: <{} ...>", node.getNodeName());
				Map<String, Object> map = this.toMap(node, binding);
				Object bean = this.bind(binding, binding.getType(), map);
				LOG.trace("bean created: {}", bean);
				this.handle(bean, binding, toContext(map));
				LOG.trace("bean saved: {}", bean);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Object bind(XMLBind binding, Class<?> type, Map<String, Object> values) {

		if (type == null || values == null || values.size() == 0) {
			return null;
		}

		Object bean = null;
		Map<String, Object> ctx = toContext(values);

		LOG.trace("context: " + ctx);

		if (binding.getSearch() != null) {
			LOG.trace("search: " + binding.getSearch());
			bean = JPA.all((Class<Model>) type).filter(binding.getSearch()).bind(ctx).fetchOne();
			LOG.trace("search found: " + bean);
			if (bean != null && !binding.isUpdate()) {
				LOG.trace("search no update");
				return bean;
			}
		}

		Mapper mapper = Mapper.of(type);
		List<XMLBind> bindings = binding.getBindings();
		boolean isNull = bean == null;
		newBean = isNull;

		if (bindings == null) {
			return bean;
		}

		if (isNull) {
			bean = newInstance(type);
		}

		LOG.trace("populate: " + type);

		for (final XMLBind bind : bindings) {

			LOG.trace("binding: " + bind);

			final String field = bind.getField();
			final String name = bind.getAlias() != null ? bind.getAlias() : field;
			final Property property = mapper.getProperty(field);

			if (property == null) { // handle dummy binding
				//TODO: this.handleDummyBind(bind, values);
				continue;
			}

			if (property.isPrimary() || property.isVirtual()) {
				continue;
			}

			Object value = values.get(name);

			LOG.trace("value: " + value);
			LOG.trace("condition: " + bind.getCondition());

			if (newBean == false && bind.getConditionEmpty() == Boolean.TRUE && property.get(bean) != null) {
				LOG.trace("field is not empty");
				continue;
			}

			if (!validate(bind, value, ctx)) {
				LOG.trace("condition failed");
				continue;
			}

			// process eval expression
			if (bind.getExpression() != null) {
				LOG.trace("expression: " + bind.getExpression());
				// default value is already computed so only do eval for node binding
				value = bind.getNode() == null ? value : bind.eval(ctx);
				LOG.trace("value: " + value);
			}

			if (value instanceof Model) {
				// do nothing
			} else if (property.isReference()) {
				value = relational(property, bind, value, ctx);
			} else if (property.isCollection() && value != null) {
				if (!(value instanceof List)) {
					value = Lists.newArrayList(value);
				}
				List<Object> items = Lists.newArrayList();
				for(Object item : (List<?>) value) {
					items.add(relational(property, bind, item, ctx));
				}
				value = items;
			}

			LOG.trace("set value: {} = {}", property.getName(), value);
			isNull = false;
			property.set(bean, value);
		}

		return isNull ? null : bean;
	}

	@SuppressWarnings("all")
	private Object relational(Property property, XMLBind bind, Object value, Map<String, Object> ctx) {

		Map<String, Object> values = ctx;
		if (value instanceof Map) {
			values = (Map) value;
		}

		Object result = bind(bind, property.getTarget(), values);

		if (result instanceof Model && (
				property.getType() == PropertyType.MANY_TO_ONE ||
				property.getType() == PropertyType.MANY_TO_MANY)) {
			if (!JPA.em().contains(result)) {
				result = JPA.manage((Model) result);
			}
		}
		return result;
	}

	private Map<String, Object> toContext(Map<String, Object> map) {
		Map<String, Object> ctx = Maps.newHashMap();
		if (context != null) {
			ctx.putAll(context);
		}
		if (map != null) {
			ctx.putAll(map);
		}
		return ctx;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean validate(XMLBind binding, Object value, Map<String, Object> values) {
		Map<String, Object> ctx = toContext(value instanceof Map ? ((Map) value) : values);
		return binding.validate(ctx);
	}

	@SuppressWarnings("all")
	private Map<String, Object> toMap(Node node, XMLBind binding) {

		Map<String, Object> map = Maps.newHashMap();

		// first prepare complete map
		for(XMLBind bind : binding.getBindings()) {
			String name = bind.getAlias();
			String path = bind.getNode();
			if (name == null) {
				name = bind.getField();
			}

			if (name == null) {
				continue;
			}

			List<Node> nodes = find(node, bind, ".");
			Object value = value(nodes, bind);

			value = this.adapt(bind, value, map);

			if (!validate(bind, value, map)) {
				continue;
			}

			// get default value
			if (bind.getNode() == null && bind.getExpression() != null) {
				value = bind.eval(toContext(map));
			}

			map.put(name, value);
		}
		return map;
	}

	private Object value(List<Node> nodes, final XMLBind bind) {
		List<Object> result = Lists.transform(nodes, new Function<Node, Object>() {
			@Override
			public Object apply(@Nullable Node input) {
				if (bind.getBindings() != null) {
					return toMap(input, bind);
				}
				if (input.getNodeType() == Node.ELEMENT_NODE) {
					Node child = input.getFirstChild();
					if (child.getNodeType() == Node.TEXT_NODE) {
						return child.getNodeValue();
					}
					return toMap(input, bind);
				}
				return input.getNodeValue();
			}
		});
		if (result.size() == 1) {
			return result.get(0);
		}
		return result.size() == 0 ? null : result;
	}

	private Object adapt(XMLBind bind, Object value, Map<String, Object> ctx) {
		String name = bind.getAdapter();
		if (name == null || !adapters.containsKey(name)) {
			return value;
		}
		DataAdapter adapter = adapters.get(name);
		return adapter.adapt(value, ctx);
	}

	@SuppressWarnings("unchecked")
	private List<Node> find(Node node, XMLBind bind, String prefix) {
		List<Node> nodes = Lists.newArrayList();
		String name = bind.getNode();
		String path = name;

		if (name == null) {
			return nodes;
		}

		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		if (!("/".equals(prefix))) {
			path = prefix + path;
		}

		try {
			LOG.trace("xpath: " + path);
			JXPathContext context = JXPathContext.newContext(node);
			nodes = context.selectNodes(path);
			LOG.trace("xpath match: " + nodes.size());
		} catch (Exception e) {
			LOG.error("Invalid xpath expression: {}", path);
		}
		return nodes;
	}

	private Object newInstance(Class<?> type) {
		try {
			return type.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}
