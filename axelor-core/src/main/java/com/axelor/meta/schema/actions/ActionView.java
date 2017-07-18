/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.meta.schema.actions;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.i18n.I18n;
import com.axelor.meta.ActionHandler;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Resource;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Collections2;
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
	private Boolean home;

	@XmlElement
	private String domain;

	@XmlElement(name = "view")
	private List<View> views;

	@XmlElement(name = "context")
	private List<Context> contexts;

	@XmlElement(name = "view-param")
	private List<ActionView.Param> params;

	@JsonGetter("title")
	public String getLocalizedTitle() {
		return I18n.get(title);
	}
	
	@JsonIgnore
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getIcon() {
		return icon;
	}

	public Boolean getHome() {
		return home;
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
			map.put("name", handler.evaluate(elem.getName()));
			map.put("type", elem.getType());

			if (viewType == null) {
				viewType = elem.getType();
			}

			items.add(map);
		}

		if (contexts != null) {
			for(Context ctx : contexts) {
				if(ctx.test(handler)) {
					Object value = handler.evaluate(ctx.getExpression());
					if (ctx.getCanCopy() == Boolean.TRUE && value instanceof Model) {
						value = JPA.copy((Model)value, true);
					}
					if (value instanceof Model && JPA.em().contains(value)) {
						value = Resource.toMapCompact(value);
					}
					if (value instanceof Collection) {
						value = Collections2.transform((Collection<?>) value, item -> {
							return item instanceof Model && JPA.em().contains(item) ? Resource.toMapCompact(item) : item;
						});
					}
					context.put(ctx.getName(), value);

					// make it available to the evaluation context
					if (ctx.getName().startsWith("_")) {
						handler.getContext().put(ctx.getName(), value);
					}
				}
			}
		}

		if (!context.containsKey("_id") && handler.getContext().containsKey("id")) {
			context.put("_id", handler.evaluate("#{id}"));
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
		if (domain != null && (domain.contains("$") || (domain.startsWith("#{") && domain.endsWith("}")))) {
			domain = handler.evaluate(toExpression(domain, true)).toString();
		}

		String title = this.getLocalizedTitle();
		if (title != null && (title.contains("$") || (title.startsWith("#{") && title.endsWith("}")))) {
			title = handler.evaluate(toExpression(title, true)).toString();
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

	/**
	 * Return an instance of {@link ActionViewBuilder} that can be used to
	 * quickly define {@link ActionView}.
	 * 
	 * @param title
	 *            the view title
	 * @return an instance of {@link ActionViewBuilder}
	 */
	public static ActionViewBuilder define(String title) {
		return new ActionViewBuilder(title);
	}

	/**
	 * The {@link ActionViewBuilder} can be used to quickly define
	 * {@link ActionView} manually, especially when setting view to
	 * {@link ActionResponse}.
	 * 
	 */
	public static final class ActionViewBuilder {

		private ActionView view = new ActionView();
		private Map<String, Object> context = Maps.newHashMap();

		private ActionViewBuilder(String title) {
			view.title = title;
			view.views = Lists.newArrayList();
			view.params = Lists.newArrayList();
			view.contexts = Lists.newArrayList();
		}
		
		public ActionViewBuilder name(String name) {
			view.setName(name);
			return this;
		}

		public ActionViewBuilder model(String model) {
			view.setModel(model);
			return this;
		}
		
		public ActionViewBuilder icon(String icon) {
			view.icon = icon;
			return this;
		}

		public ActionViewBuilder add(String type) {
			return add(type, null);
		}

		public ActionViewBuilder add(String type, String name) {
			View item = new View();
			item.setType(type);
			item.setName(name);
			view.views.add(item);
			return this;
		}
		
		public ActionViewBuilder domain(String domain) {
			view.domain = domain;
			return this;
		}
		
		public ActionViewBuilder context(String key, Object value) {
			this.context.put(key, value);
			if (value instanceof String) {
				Context context = new Context();
				context.setName(key);
				context.setExpression((String) value);
				view.contexts.add(context);
			}
			return this;
		}
		
		public ActionViewBuilder param(String key, String value) {
			Param item = new Param();
			item.name = key;
			item.value = value;
			view.params.add(item);
			return this;
		}

		/**
		 * Get the prepared {@link ActionView}.
		 * 
		 * @return an instance of {@link ActionView}
		 */
		public ActionView get() {
			return view;
		}

		/**
		 * Return a {@link Map} that represents the action view.
		 * 
		 * @return a {@link Map}
		 */
		public Map<String, Object> map() {
			Map<String, Object> result = Maps.newHashMap();
			Map<String, Object> params = Maps.newHashMap();
			List<Object> items = Lists.newArrayList();
			String type = null;

			for (View v : view.views) {
				if (type == null) {
					type = v.type;
				}
				Map<String, Object> item = Maps.newHashMap();
				item.put("type", v.getType());
				item.put("name", v.getName());
				items.add(item);
			}

			if (type == null) {
				type = "grid";
				items.add(ImmutableMap.of("type", "grid"));
				items.add(ImmutableMap.of("type", "form"));
			}

			for(Param param : view.params) {
				Object value = param.value;
				if ("false".equals(value)) value = false;
				if ("true".equals(value)) value = true;
				params.put(param.name, value);
			}

			result.put("title", view.title);
			result.put("icon", view.icon);
			result.put("model", view.getModel());
			result.put("viewType", type);
			result.put("views", items);
			result.put("domain", view.domain);
			result.put("context", context);
			result.put("params", params);
			
			return result;
		}
	}
}
