package com.axelor.meta.views;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.ActionHandler;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@XmlType(name = "AbstractAction")
public abstract class Action {
	
	protected final transient Logger log = LoggerFactory.getLogger(Action.class);
	
	@XmlAttribute
	private String name;
	
	@JsonIgnore
	@XmlElements({
		@XmlElement(name = "error", type = Error.class),
		@XmlElement(name = "alert", type = Alert.class),
		@XmlElement(name = "field", type = Field.class),
		@XmlElement(name = "attribute", type = Attr.class),
		@XmlElement(name = "call", type = Call.class),
		@XmlElement(name = "view", type = View.class),
		@XmlElement(name = "context", type = Context.class),
	})
	private List<? extends Act> elements;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<? extends Act> getElements() {
		return elements;
	}
	
	public void setElements(List<? extends Act> elements) {
		this.elements = elements;
	}
	
	public abstract Object wrap(ActionHandler handler);
	
	public abstract Object evaluate(ActionHandler handler);
	
	@XmlType
	public static class ActionRecord extends Action {
		
		@XmlAttribute
		private String model;
		
		@XmlAttribute
		private String search;
		
		@XmlAttribute
		private String ref;
		
		@XmlAttribute(name = "copy")
		private Boolean canCopy;
		
		public String getModel() {
			return model;
		}
		
		public void setModel(String model) {
			this.model = model;
		}
		
		public String getSearch() {
			return search;
		}
		
		public void setSearch(String search) {
			this.search = search;
		}
		
		public String getRef() {
			return ref;
		}
		
		public void setRef(String ref) {
			this.ref = ref;
		}
		
		public Boolean getCanCopy() {
			return canCopy;
		}
		
		public void setCanCopy(Boolean canCopy) {
			this.canCopy = canCopy;
		}
		
		private Class<?> findClass(String name) {
			try {
				return Class.forName(name);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public Object evaluate(ActionHandler handler) {
			Map<String, Object> map = Maps.newHashMap();
			return _evaluate(handler, map);
		}
		
		private Object _evaluate(ActionHandler handler, Map<String, Object> map) {
			
			Class<?> entityClass = findClass(model);
			if (ref != null) {
				Object result = handler.evaluate(ref);
				if (result != null) {
					if (canCopy == Boolean.TRUE) {
						return JPA.copy((Model)result, true);
					}
					return result;
				}
			}
			
			Mapper mapper = Mapper.of(entityClass);
			Object target = Mapper.toBean(entityClass, null);
			
			for(Act field : getElements()) {
				
				if (!field.test(handler)) {
					continue;
				}
			
				Property property = mapper.getProperty(field.getName());
				if (property == null) {
					log.error("No such field: {}", field.getName());
					continue;
				}
				
				String expr = field.getExpression();
				Object value = expr;

				try {
					value = handler.evaluate(expr);
				} catch (Exception e) {
					log.error("error evaluating expression");
					log.error("expression: {}", expr, e);
					continue;
				}
				
				if (((Field) field).getCanCopy() == Boolean.TRUE && value instanceof Model) {
					value = JPA.copy((Model) value, true);
				}
				
				try {
					property.set(target, value);
					if (map != null) {
						map.put(property.getName(), property.get(target));
					}
				} catch (Exception e) {
					log.error("invalid value for field: {}", property.getName());
					log.error("value: {}", value);
					continue;
				}
				
			}
			
			if (search != null) {
				Object result = handler.search(entityClass, search, map);
				if (result != null) {
					if (canCopy == Boolean.TRUE) {
						return JPA.copy((Model)result, true);
					}
					return result;
				}
			}
			
			return target;
		}
		
		@Override
		public Object wrap(ActionHandler handler) {
			Map<String, Object> map = Maps.newHashMap();
			Object value = _evaluate(handler, map);
			if (value == null) {
				return null;
			}
			return ImmutableMap.of("values", map);
		}
	}

	@XmlType
	public static class ActionAttrs extends Action {
		
		@Override @SuppressWarnings("all")
		public Object evaluate(ActionHandler handler) {
			
			Map<String, Object> map = Maps.newHashMap();
			for(Act attr : getElements()) {
				if (!attr.test(handler)) continue;
				Map<String, Object> attrs = (Map) map.get(attr.getField());
				if (attrs == null) {
					attrs = Maps.newHashMap();
					map.put(attr.getField(), attrs);
				}
				
				String name = attr.getName();
				Object value = null;
				if (name.matches("readonly|required|recommend|hidden|collapse")) {
					value = attr.test(handler, attr.getExpression());
				} else {
					value = handler.evaluate(attr.getExpression());
				}
				attrs.put(attr.getName(), value);
			}
			return map;
		}
		
		@Override
		public Object wrap(ActionHandler handler) {
			Object value = evaluate(handler);
			if (value == null) {
				return null;
			}
			return ImmutableMap.of("attrs", value);
		}
	}
	
	@XmlType
	public static class ActionValidate extends Action {
		
		@Override
		public Object evaluate(ActionHandler handler) {
			for(Act validator : getElements()) {
				if (validator.test(handler)) {
					String key = validator.getClass().getSimpleName().toLowerCase();
					String val = validator.getMessage();
					
					if (!Strings.isNullOrEmpty(val))
						val = handler.evaluate("eval: " + "\"\"\"" + val + "\"\"\"").toString();
					
					Map<String, Object> result = Maps.newHashMap();
					result.put(key, val);
					return result;
				}
			}
			return null;
		}
		
		@Override
		public Object wrap(ActionHandler handler) {
			return evaluate(handler);
		}
	}
	
	@XmlType
	public static class ActionMethod extends Action {
		
		private boolean isRpc(String methodCall) {
			return Pattern.matches("(\\w+)\\((.*?)\\)", methodCall);
		}
		
		@Override
		public Object evaluate(ActionHandler handler) {
			Call method = (Call) getElements().get(0);
			if (isRpc(method.getMethod()))
				return handler.rpc(method.getController(), method.getMethod());
			return handler.call(method.getController(), method.getMethod());
		}
		
		@Override
		public Object wrap(ActionHandler handler) {
			return evaluate(handler);
		}
	}
	
	@XmlType
	public static class ActionView extends Action {
		
		@XmlAttribute
		private String title;
		
		@XmlAttribute
		private String model;
		
		@XmlElement
		private String domain;
		
		public String getTitle() {
			return title;
		}
		
		public String getModel() {
			return model;
		}
		
		public String getDomain() {
			return domain;
		}
		
		@XmlTransient
		public Object getContext() {
			return null;
		}
		
		@Override
		public Object evaluate(ActionHandler handler) {
			Map<String, Object> result = Maps.newHashMap();
			Map<String, Object> context = Maps.newHashMap();
			List<Object> views = Lists.newArrayList();
			
			String viewType = null;
			
			for(Act elem : this.getElements()) {

				if (!elem.test(handler))
					continue;
				
				if (elem instanceof View) {
					Map<String, Object> map = Maps.newHashMap();
					map.put("name", elem.getName());
					map.put("type", ((View) elem).getType());

					if (viewType == null)
						viewType = ((View) elem).getType();
					
					views.add(map);
				}
				
				if (elem instanceof Field) {
					Object value = handler.evaluate(elem.getExpression());
					if (((Field) elem).getCanCopy() == Boolean.TRUE && value instanceof Model) {
						value = JPA.copy((Model)value, true);
					}
					context.put(elem.getName(), value);
				}
			}
			
			String domain = this.getDomain();
			
			if (domain != null && domain.contains("$")) {
				domain = handler.evaluate("eval: \"" + domain + "\"").toString();
			}
			
			result.put("title", getTitle());
			result.put("model", getModel());
			result.put("viewType", viewType);
			result.put("views", views);
			result.put("domain", domain);
			result.put("context", context);
			
			return result;
		}
		
		@Override
		public Object wrap(ActionHandler handler) {
			return ImmutableMap.of("view", evaluate(handler));
		}
	}
	
	@XmlType
	public static abstract class Act {
		
		@XmlAttribute(name = "if")
		private String condition;

		@XmlAttribute
		private String name;
		
		@XmlAttribute(name = "expr")
		private String expression;
		
		@XmlAttribute(name = "message")
		private String message;

		@XmlAttribute(name = "for")
		private String field;
		
		public String getCondition() {
			return condition;
		}
		
		public void setCondition(String condition) {
			this.condition = condition;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getExpression() {
			return expression;
		}
		
		public void setExpression(String expression) {
			this.expression = expression;
		}
		
		public String getMessage() {
			return message;
		}
		
		public void setMessage(String message) {
			this.message = message;
		}
		
		public String getField() {
			return field;
		}
		
		public void setField(String field) {
			this.field = field;
		}
		
		boolean test(ActionHandler handler, String expression) {
			if (Strings.isNullOrEmpty(expression)) // if expression is not given always return true
				return true;
			if (expression.matches("true"))
				return true;
			if (expression.equals("false"))
				return false;
			if (expression != null && !expression.matches("^(eval|select|action):")) {
				expression = "eval:" + expression;
			}
			Object result = handler.evaluate(expression);
			if (result == null)
				return false;
			if (result instanceof Number && result.equals(0))
				return false;
			if (result instanceof Boolean)
				return (Boolean) result;
			return true;
		}
		
		boolean test(ActionHandler handler) {
			return test(handler, getCondition());
		}
	}
	
	@XmlType(name = "ActError")
	public static class Error extends Act {
	}
	
	@XmlType(name = "ActAlert")
	public static class Alert extends Error {
	}
	
	@XmlType(name = "ActField")
	public static class Field extends Act {
		
		@XmlAttribute(name = "copy")
		private Boolean canCopy;
		
		public Boolean getCanCopy() {
			return canCopy;
		}
	}
	
	@XmlType(name = "ActAttr")
	public static class Attr extends Act {
	}
	
	@XmlType(name = "ActCall")
	public static class Call extends Act {
		
		@XmlAttribute
		private String method;
		
		@XmlAttribute(name = "class")
		private String controller;
		
		public String getMethod() {
			return method;
		}
		
		public void setMethod(String method) {
			this.method = method;
		}
		
		public String getController() {
			return controller;
		}
		
		public void setController(String controller) {
			this.controller = controller;
		}
	}
	
	@XmlType(name = "ActView")
	public static class View extends Act {
		
		@XmlAttribute
		private String type;
		
		public String getType() {
			return type;
		}
	}
	
	@XmlType(name = "ActContext")
	public static class Context extends Field {
		
	}
}
