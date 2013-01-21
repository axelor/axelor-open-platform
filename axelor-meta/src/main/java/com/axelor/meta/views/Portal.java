package com.axelor.meta.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("portal")
public class Portal extends AbstractView {
	
	@XmlAttribute
	private Integer cols;

	@XmlElements({
		@XmlElement(name = "portlet", type = Portlet.class)
	})
	private List<Portlet> items;

	public Integer getCols() {
		return cols;
	}
	
	public void setCols(Integer cols) {
		this.cols = cols;
	}
	
	public List<Portlet> getItems() {
		return items;
	}
	
	public void setItems(List<Portlet> items) {
		this.items = items;
	}
}
