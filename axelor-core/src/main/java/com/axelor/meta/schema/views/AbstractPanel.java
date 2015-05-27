/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public abstract class AbstractPanel extends AbstractContainer {

	@XmlAttribute
	private Integer itemSpan;

	@XmlAttribute
	private Boolean showFrame;

	@XmlAttribute
	private Boolean sidebar;

	@XmlAttribute
	private Boolean stacked;

	@XmlAttribute
	private Boolean attached;

	@XmlAttribute
	private String onTabSelect;

	public Integer getItemSpan() {
		return itemSpan;
	}

	public Boolean getShowFrame() {
		return showFrame;
	}

	public Boolean getSidebar() {
		return sidebar;
	}

	public Boolean getStacked() {
		return stacked;
	}

	public Boolean getAttached() {
		return attached;
	}

	public String getOnTabSelect() {
		return onTabSelect;
	}

	protected List<AbstractWidget> process(List<AbstractWidget> items) {
		if (items == null) {
			items = new ArrayList<>();
		}
		for (AbstractWidget item : items) {
			item.setModel(getModel());
		}
		return items;
	}
}
