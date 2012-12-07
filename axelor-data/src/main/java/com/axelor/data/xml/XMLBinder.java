package com.axelor.data.xml;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
	
	private Map<String, XMLAdapter> adapters = Maps.newHashMap();

	private XPath xpath = XPathFactory.newInstance().newXPath();

	public XMLBinder(XMLInput input, Map<String, Object> context) {
		this.input = input;
		this.context = context;
	}
	
	public void registerAdapter(XMLAdapter adapter) {
		adapters.put(adapter.getName(), adapter);
	}
	
	protected abstract void handle(Object bean, XMLBind bind);
	
	public void bind(Document element) {
		
		for(XMLBind binding : input.getBindings()) {
			List<Node> nodes = this.find(element, binding, "/");
			for(Node node : nodes) {
				Map<String, Object> map = this.toMap(node, binding);
				Object bean = this.bind(binding, binding.getType(), map);
				this.handle(bean, binding);
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

		if (binding.getSearch() != null) {
			bean = JPA.all((Class<Model>) type).filter(binding.getSearch()).bind(ctx).fetchOne();
			if (bean != null && !binding.isUpdate()) {
				return bean;
			}
		}
		
		Mapper mapper = Mapper.of(type);
		List<XMLBind> bindings = binding.getBindings();
		boolean isNull = bean == null;
		
		if (bindings == null) {
			return bean;
		}

		if (isNull) {
			bean = newInstance(type);
		}

		for (final XMLBind bind : bindings) {
			
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

			if (!validate(bind, value, ctx)) {
				continue;
			}

			// get default value
			if (bind.getNode() == null && bind.getExpression() != null) {
				value = bind.eval(ctx);
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

			if (bind.getNode() == null && bind.getExpression() != null) {
				value = bind.eval(map);
			}

			if (validate(bind, value, map)) {
				map.put(name, value);
			}
		}
		return map;
	}
	
	private Object value(List<Node> nodes, final XMLBind bind) {
		List<Object> result = Lists.transform(nodes, new Function<Node, Object>() {
			@Override
			public Object apply(@Nullable Node input) {
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
		XMLAdapter adapter = adapters.get(name);
		return adapter.adapt(value, ctx);
	}
	
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
			XPathExpression expression = xpath.compile(path);
			NodeList items = (NodeList) expression.evaluate(node, XPathConstants.NODESET);
			for (int i = 0; i < items.getLength(); i++) {
				nodes.add(items.item(i));
			}
		} catch (XPathExpressionException e) {
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
