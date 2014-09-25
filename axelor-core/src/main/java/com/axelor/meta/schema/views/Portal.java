/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
@JsonTypeName("portal")
public class Portal extends AbstractView {
	
	@XmlAttribute
	private Integer cols;

	@XmlElements({
		@XmlElement(name = "portlet", type = Portlet.class),
		@XmlElement(name = "tabs", type = PortalTabs.class)
	})
	private List<AbstractWidget> items;

	public Integer getCols() {
		return cols;
	}
	
	public void setCols(Integer cols) {
		this.cols = cols;
	}
	
	public List<AbstractWidget> getItems() {
		return items;
	}

	@XmlType
	@JsonTypeName("tabs")
	public static class PortalTabs extends AbstractContainer {

		@XmlElement(name = "tab")
		private List<Portal> tabs;
		
		public List<Portal> getTabs() {
			return tabs;
		}

		public void setTabs(List<Portal> tabs) {
			this.tabs = tabs;
		}
	}
}
