package com.axelor.meta.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlTransient
public abstract class SimpleContainer extends AbstractContainer {

	@XmlAttribute
	private Integer cols;

	@XmlAttribute
	private String colWidths;
	
	@XmlElements({
		@XmlElement(name = "group", type = Group.class),
        @XmlElement(name = "notebook", type = Notebook.class),
        @XmlElement(name = "field", type = Field.class),
        @XmlElement(name = "break", type = Break.class),
        @XmlElement(name = "spacer", type = Spacer.class),
        @XmlElement(name = "separator", type = Separator.class),
        @XmlElement(name = "label", type = Label.class),
        @XmlElement(name = "button", type = Button.class)
	})
	private List<AbstractWidget> items;

	public Integer getCols() {
		return cols;
	}

	public void setCols(Integer cols) {
		this.cols = cols;
	}

	public String getColWidths() {
		return colWidths;
	}

	public void setColWidths(String colWidths) {
		this.colWidths = colWidths;
	}
	
	public List<AbstractWidget> getItems() {
		for (AbstractWidget abstractWidget : items) {
			abstractWidget.setModel(super.getModel());
		}
		return items;
	}
	
	public void setItems(List<AbstractWidget> items) {
		this.items = items;
	}
}
