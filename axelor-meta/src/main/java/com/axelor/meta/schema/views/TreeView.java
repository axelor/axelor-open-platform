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

		public List<NodeField> getFields() {
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
