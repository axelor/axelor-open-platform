/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("panel")
public class Panel extends AbstractPanel {

	@XmlAttribute
	private Boolean canCollapse;

	@XmlAttribute
	private String collapseIf;

	@XmlElement
	private Menu menu;

	@XmlElements({
		@XmlElement(name = "field", type = PanelField.class),
		@XmlElement(name = "spacer", type = Spacer.class),
		@XmlElement(name = "label", type = Label.class),
		@XmlElement(name = "static", type = Static.class),
		@XmlElement(name = "help", type = Help.class),
		@XmlElement(name = "button", type = Button.class),
		@XmlElement(name = "button-group", type = ButtonGroup.class),
		@XmlElement(name = "panel", type = Panel.class),
		@XmlElement(name = "panel-related", type = PanelRelated.class),
		@XmlElement(name = "panel-dashlet", type = Dashlet.class),
		@XmlElement(name = "panel-include", type = PanelInclude.class)
	})
	private List<AbstractWidget> items;

	public Boolean getCanCollapse() {
		return canCollapse;
	}

	public void setCanCollapse(Boolean canCollapse) {
		this.canCollapse = canCollapse;
	}

	public String getCollapseIf() {
		return collapseIf;
	}

	public void setCollapseIf(String collapseIf) {
		this.collapseIf = collapseIf;
	}

	public Menu getMenu() {
		return menu;
	}

	public void setMenu(Menu menu) {
		this.menu = menu;
	}

	public List<AbstractWidget> getItems() {
		return process(items);
	}

	public void setItems(List<AbstractWidget> items) {
		this.items = items;
	}
}
