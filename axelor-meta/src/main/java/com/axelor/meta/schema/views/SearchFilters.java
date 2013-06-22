package com.axelor.meta.schema.views;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Maps;

@XmlType
@JsonTypeName("search-filters")
public class SearchFilters extends AbstractView {

	@XmlElement(name = "filter")
	private List<SearchFilter> filters;
	
	public List<SearchFilter> getFilters() {
		return filters;
	}

	@XmlType
	@JsonInclude(Include.NON_NULL)
	public static class SearchFilter {

		@XmlAttribute
		private String title;

		@XmlElement
		private String domain;

		@JsonIgnore
		@XmlElement(name = "context")
		private List<SearchContext> contexts;
		
		public String getTitle() {
			return title;
		}
		
		public String getDomain() {
			return domain;
		}

		public Map<String, Object> getContext() {
			if (contexts == null || contexts.isEmpty()) {
				return null;
			}
			Map<String, Object> context = Maps.newHashMap();
			for(SearchContext ctx : contexts) {
				context.put(ctx.getName(), ctx.getValue());
			}
			return context;
		}
	}

	@XmlType
	public static class SearchContext {
		
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
}
