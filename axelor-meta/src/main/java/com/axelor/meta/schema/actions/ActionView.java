package com.axelor.meta.schema.actions;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.meta.ActionHandler;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@XmlType(propOrder = {
	"views",
	"params",
	"domain",
	"contexts"
})
public class ActionView extends Action {
	
	@XmlType
	public static class View extends Element {
		
		@XmlAttribute
		private String type;
		
		public String getType() {
			return type;
		}
		
		public void setType(String type) {
			this.type = type;
		}
	}
	
	@XmlType
	public static class Context extends ActionRecord.RecordField {
		
	}
	
	@XmlType
	public static class Param {
		
		@XmlAttribute
		private String name;
		
		@XmlAttribute
		private String value;
		
		public String getName() {
			return name;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	@XmlAttribute
	private String title;
	
	@XmlAttribute
	private String icon;
	
	@XmlAttribute
	private String model;
	
	@XmlElement
	private String domain;
	
	@XmlElement(name = "view")
	private List<View> views;
	
	@XmlElement(name = "context")
	private List<Context> contexts;
	
	@XmlElement(name = "view-param")
	private List<ActionView.Param> params;
	
	public String getTitle() {
		return JPA.translate(title);
	}
	
	public String getIcon() {
		return icon;
	}

	public String getModel() {
		return model;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public List<View> getViews() {
		return views;
	}
	
	public void setViews(List<View> views) {
		this.views = views;
	}

	@XmlTransient
	public Object getContext() {
		return null;
	}
	
	public List<Param> getParams() {
		return params;
	}
	
	public void setParams(List<ActionView.Param> params) {
		this.params = params;
	}

	@Override
	public Object evaluate(ActionHandler handler) {
		Map<String, Object> result = Maps.newHashMap();
		Map<String, Object> context = Maps.newHashMap();
		Map<String, Object> viewParams = Maps.newHashMap();
		List<Object> items = Lists.newArrayList();
		
		String viewType = null;
		
		for(View elem : views) {

			if (!elem.test(handler)) {
				continue;
			}
			
			Map<String, Object> map = Maps.newHashMap();
			map.put("name", elem.getName());
			map.put("type", elem.getType());

			if (viewType == null) {
				viewType = elem.getType();
			}

			items.add(map);
		}
		
		if (contexts != null) {
			for(Context ctx : contexts) {
				Object value = handler.evaluate(ctx.getExpression());
				if (ctx.getCanCopy() == Boolean.TRUE && value instanceof Model) {
					value = JPA.copy((Model)value, true);
				}
				context.put(ctx.getName(), value);
			}
		}

		if (params != null) {
			for(Param param : params) {
				Object value = param.value;
				if ("false".equals(value)) value = false;
				if ("true".equals(value)) value = true;
				viewParams.put(param.name, value);
			}
		}
		
		String domain = this.getDomain();
		if (domain != null && domain.contains("$")) {
			domain = handler.evaluate("eval: \"" + domain + "\"").toString();
		}
		
		String title = this.getTitle();
		if (title != null && title.contains("$")) {
			title = handler.evaluate("eval: \"" + title + "\"").toString();
		}

		result.put("title", title);
		result.put("icon", getIcon());
		result.put("model", getModel());
		result.put("viewType", viewType);
		result.put("views", items);
		result.put("domain", domain);
		result.put("context", context);
		result.put("params", viewParams);

		return result;
	}
	
	@Override
	public Object wrap(ActionHandler handler) {
		return ImmutableMap.of("view", evaluate(handler));
	}
}