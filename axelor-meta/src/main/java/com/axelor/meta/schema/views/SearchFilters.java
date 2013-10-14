/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.meta.schema.views;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
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

		@JsonIgnore
		public String getDefaultTitle() {
			return title;
		}

		public String getTitle() {
			return JPA.translate(title, title, model, "filter");
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
