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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("group")
public class Group extends SimpleContainer {

	@XmlAttribute
	private Boolean canCollapse;

	@XmlAttribute
	private String collapseIf;

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
}
