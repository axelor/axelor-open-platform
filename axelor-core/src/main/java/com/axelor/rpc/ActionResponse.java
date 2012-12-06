package com.axelor.rpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@XmlType
@XmlRootElement(name = "response")
public class ActionResponse extends Response {

	Map<String, Object> _data = Maps.newHashMap();
	
	@SuppressWarnings("all")
	private void set(String name, Object value) {
		if (getData() == null) {
			List<Object> data = Lists.newArrayList();
			data.add(_data);
			setData(data);
		}
		_data.put(name, value);
	}
	
	public void setFlash(String flash) {
		set("flash", flash);
	}

	public void setValues(Object context) {
		set("values", context);
	}

	public void setView(Map<String, String> view) {
		set("view", view);
	}
	
	public void setView(String title, String entity, String mode, String criteria) {
		setView(ImmutableMap.of("title", title, "entity", entity, "view", mode, "criteria", criteria));
	}
	
	public void setAttrs(Map<String, Map<String, Object>> attrs) {
		set("attrs", attrs);
	}
	
	@SuppressWarnings("all")
	private void setAttr(String fieldName, String attr, Object value) {
		
		Map<String, Map<String, Object>> attrs = null;
		
		try {
			attrs = (Map) ((Map) getItem(0)).get("attrs");
		}catch(Exception e) {
		}

		if (attrs == null) {
			attrs = new HashMap<String, Map<String,Object>>();
		}
		
		Map<String, Object> my = attrs.get(fieldName);
		if (my == null)
			my = new HashMap<String, Object>();
		
		my.put(attr, value);
		attrs.put(fieldName, my);
		
		setAttrs(attrs);
	}
	
	public void setReadonly(String fieldName, boolean readonly) {
		setAttr(fieldName, "readonly", readonly);
	}
	
	public void setHidden(String fieldName, boolean hidden) {
		setAttr(fieldName, "hidden", hidden);
	}
	
	public void setColor(String fieldName, String color) {
		setAttr(fieldName, "color", color);
	}
}
