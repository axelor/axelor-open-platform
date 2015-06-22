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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.schema.views.Search.SearchField;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("tree")
public class TreeView extends AbstractView {

	@XmlAttribute
	private Boolean showHeader;

	@XmlElement(name = "column")
	private List<TreeColumn> columns;

	@XmlElement(name = "node")
	private List<Node> nodes;

	public Boolean getShowHeader() {
		return showHeader;
	}

	public List<TreeColumn> getColumns() {
		return columns;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	@XmlType
	@JsonInclude(Include.NON_NULL)
	public static class TreeColumn extends SearchField {

	}

	@XmlType
	@JsonInclude(Include.NON_NULL)
	public static class Node {

		@XmlAttribute
		private String model;

		@XmlAttribute
		private String parent;

		@XmlAttribute
		private String onClick;

		@XmlAttribute
		private Boolean draggable;

		@XmlAttribute
		private String domain;

		@XmlAttribute
		private String orderBy;

		@XmlElements({
			@XmlElement(name = "field", type = NodeField.class),
			@XmlElement(name = "button", type = Button.class)
		})
		private List<AbstractWidget> items;

		public String getModel() {
			return model;
		}

		public String getParent() {
			return parent;
		}

		public String getOnClick() {
			return onClick;
		}

		public Boolean getDraggable() {
			return draggable;
		}

		public String getDomain() {
			return domain;
		}

		public String getOrderBy() {
			return orderBy;
		}

		public List<AbstractWidget> getItems() {
			if (items != null) {
				for(AbstractWidget item : items) {
					item.setModel(model);
				}
			}
			return items;
		}
	}

	@XmlType
	@JsonInclude(Include.NON_NULL)
	public  static class NodeField extends Field {

		@XmlAttribute
		private String as;

		public String getAs() {
			return as;
		}

		@Override
		public String getSelection() {
			String selection = super.getSelection();
			Class<?> klass = null;
			if (selection != null) {
				return selection;
			}
			try {
				klass = Class.forName(getModel());
			} catch (ClassNotFoundException e) {
				return null;
			}
			Property property = Mapper.of(klass).getProperty(getName());
			if (property == null) {
				return null;
			}
			return property.getSelection();
		}
	}
}
