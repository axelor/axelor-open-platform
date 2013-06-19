package com.axelor.meta.schema.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("grid")
public class GridView extends AbstractView {

	@XmlAttribute
    private Boolean expandable;

	@XmlAttribute
	private String orderBy;
	
	@XmlAttribute
	private String groupBy;
	
	private Hilite hilite;

	@XmlElements({
		@XmlElement(name="field", type=Field.class),
		@XmlElement(name="button", type=Button.class)
	})
	private List<AbstractWidget> items;
	
	public Boolean getExpandable() {
		return expandable;
	}
	
	public void setExpandable(Boolean expandable) {
		this.expandable = expandable;
	}
	
	public String getOrderBy() {
		return orderBy;
	}
	
	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}
	
	public String getGroupBy() {
		return groupBy;
	}
	
	public void setGroupBy(String groupBy) {
		this.groupBy = groupBy;
	}
	
	public Hilite getHilite() {
		return hilite;
	}
	
	public void setHilite(Hilite hilite) {
		this.hilite = hilite;
	}
	
	public List<AbstractWidget> getItems() {
		if(items != null) {
			for (AbstractWidget field : items) {
				field.setModel(super.getModel());
			}
		}
		return items;
	}
	
	public void setItems(List<AbstractWidget> items) {
		this.items = items;
	}
}
