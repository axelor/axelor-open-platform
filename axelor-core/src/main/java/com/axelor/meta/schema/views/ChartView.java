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
package com.axelor.meta.schema.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.axelor.i18n.I18n;
import com.axelor.meta.schema.views.Search.SearchField;
import com.fasterxml.jackson.annotation.JsonGetter;
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

	@JsonIgnore
	@XmlElement(name = "dataset")
	private DataSet dataSet;

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

	public DataSet getDataSet() {
		return dataSet;
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

		@JsonGetter("title")
		public String getLocalizedTitle() {
			return I18n.get(title);
		}

		@JsonIgnore
		public String getTitle() {
			return title;
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

		@JsonGetter("title")
		public String getLocalizedTitle() {
			return I18n.get(title);
		}

		@JsonIgnore
		public String getTitle() {
			return title;
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
