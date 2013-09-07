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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

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

		@XmlElement(name = "field")
		private List<NodeField> fields;
		
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

		public List<NodeField> getFields() {
			if (fields != null) {
				for(NodeField field : fields) {
					field.setModel(model);
				}
			}
			return fields;
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
	}
}
