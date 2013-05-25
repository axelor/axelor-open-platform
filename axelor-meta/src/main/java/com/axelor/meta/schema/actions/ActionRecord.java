package com.axelor.meta.schema.actions;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.ActionHandler;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@XmlType
public class ActionRecord extends Action {
	
	@XmlType
	public static class RecordField extends Element {
		
		@XmlAttribute(name = "copy")
		private Boolean canCopy;
		
		public Boolean getCanCopy() {
			return canCopy;
		}
	}
	
	@XmlAttribute
	private String model;
	
	@XmlAttribute
	private String search;
	
	@XmlAttribute
	private String ref;
	
	@XmlAttribute(name = "copy")
	private Boolean canCopy;
	
	@XmlElement(name = "field")
	private List<RecordField> fields;
	
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
	
	public List<RecordField> getFields() {
		return fields;
	}
	
	public void setFields(List<RecordField> field) {
		this.fields = field;
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
		
		for(RecordField recordField : fields) {
			
			if (!recordField.test(handler) || Strings.isNullOrEmpty(recordField.getName())) {
				continue;
			}
			
			for(String name : recordField.getName().split(",")){
				name = name.trim();
				Property property = mapper.getProperty(name);
				if (property == null) {
					log.error("No such field: {}", name);
					continue;
				}
				
				String expr = recordField.getExpression();
				Object value = expr;

				try {
					value = handler.evaluate(expr);
				} catch (Exception e) {
					log.error("error evaluating expression");
					log.error("expression: {}", expr, e);
					continue;
				}
				
				if (((RecordField) recordField).getCanCopy() == Boolean.TRUE && value instanceof Model) {
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