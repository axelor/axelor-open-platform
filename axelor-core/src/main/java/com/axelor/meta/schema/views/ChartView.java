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
package com.axelor.meta.schema.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.axelor.db.JPA;
import com.axelor.meta.schema.views.Search.SearchField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("chart")
public class ChartView extends AbstractView {

	@XmlAttribute
	private Boolean stacked;
	
	@XmlAttribute
	private String onInit;

	@XmlElement(name = "field", type = SearchField.class)
	@XmlElementWrapper(name = "search-fields")
	private List<SearchField> searchFields;

	@XmlElement(name = "dataset")
	private ChartQuery query;

	@XmlElement
	private ChartCategory category;

	@XmlElement
	private List<ChartSeries> series;

	@XmlElement
	private List<ChartConfig> config;

	public Boolean getStacked() {
		return stacked;
	}
	
	public String getOnInit() {
		return onInit;
	}

	public List<SearchField> getSearchFields() {
		return searchFields;
	}

	public ChartQuery getQuery() {
		return query;
	}

	public ChartCategory getCategory() {
		return category;
	}

	public List<ChartSeries> getSeries() {
		return series;
	}

	public List<ChartConfig> getConfig() {
		return config;
	}

	@XmlType
	public static class ChartQuery {

		@XmlAttribute
		private String type;

		@XmlValue
		private String text;

		public String getType() {
			return type;
		}

		public String getText() {
			return text;
		}
	}

	@XmlType
	@JsonTypeName("category")
	public static class ChartCategory {

		@XmlAttribute
		private String key;

		@XmlAttribute
		private String type;

		@XmlAttribute
		private String title;

		public String getKey() {
			return key;
		}

		public String getType() {
			return type;
		}

		@JsonIgnore
		public String getDefaultTitle() {
			return title;
		}

		public String getTitle() {
			return JPA.translate(title, title, null, "chart");
		}
	}

	@XmlType
	@JsonTypeName("series")
	public static class ChartSeries {

		@XmlAttribute
		private String key;

		@XmlAttribute
		private String groupBy;

		@XmlAttribute
		private String type;

		@XmlAttribute
		private String side;

		@XmlAttribute
		private String title;

		@XmlAttribute
		private String aggregate;

		public String getKey() {
			return key;
		}

		public String getGroupBy() {
			return groupBy;
		}

		public String getType() {
			return type;
		}

		public String getSide() {
			return side;
		}

		@JsonIgnore
		public String getDefaultTitle() {
			return title;
		}

		public String getTitle() {
			return JPA.translate(title, title, null, "chart");
		}

		public String getAggregate() {
			return aggregate;
		}
	}

	@XmlType
	public static class ChartConfig {

		@XmlAttribute
		private String name;

		@XmlAttribute
		private String value;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}
}
