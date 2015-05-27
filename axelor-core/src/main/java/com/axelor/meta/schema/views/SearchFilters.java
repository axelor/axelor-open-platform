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
package com.axelor.meta.schema.views;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.i18n.I18n;
import com.fasterxml.jackson.annotation.JsonGetter;
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
		if(filters != null) {
			for (SearchFilter searchFilter : filters) {
				searchFilter.setModel(super.getModel());
			}
		}
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
		private String model;

		@JsonIgnore
		@XmlElement(name = "context")
		private List<SearchContext> contexts;

		@JsonGetter("title")
		public String getLocalizedTitle() {
			return I18n.get(title);
		}

		@JsonIgnore
		public String getTitle() {
			return title;
		}

		public String getDomain() {
			return domain;
		}

		public void setModel(String model) {
			this.model = model;
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
