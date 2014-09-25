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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.MetaStore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("include")
public class FormInclude extends AbstractWidget {
	
	@XmlAttribute(name = "view")
	private String name;

	@XmlAttribute(name = "from")
	private String module;
	
	private transient AbstractView owner;

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getModule() {
		return module;
	}
	
	public void setModule(String module) {
		this.module = module;
	}
	
	public void setOwner(AbstractView owner) {
		this.owner = owner;
	}
	
	@JsonInclude
	public AbstractView getView() {
		AbstractView view = MetaStore.getView(name, module);
		if (view == owner) {
			return null;
		}
		return view;
	}
}
