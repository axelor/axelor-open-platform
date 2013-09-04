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
		@XmlElement(name = "portlet", type = Portlet.class),
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
		if(items != null) {
			for (AbstractWidget abstractWidget : items) {
				abstractWidget.setModel(super.getModel());
			}
		}
		return items;
	}
	
	public void setItems(List<AbstractWidget> items) {
		this.items = items;
	}
}
