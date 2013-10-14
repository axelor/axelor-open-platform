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
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlType
public class Menu {

	@XmlType
	public static class Item extends MenuItem {

		@JsonIgnore
		private String model;

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		@Override
		public String getTitle() {
			return JPA.translate(super.getDefaultTitle(), super.getDefaultTitle(), model, "button");
		}

	}

	@XmlAttribute
	private String title;

	@XmlAttribute
	private String icon;

	@XmlAttribute
	private Boolean showTitle;

	@XmlElements({ @XmlElement(name = "item"), @XmlElement(name = "divider") })
	private List<Item> items;

	@JsonIgnore
	private String model;

	@JsonIgnore
	public String getDefaultTitle() {
		return title;
	}

	public String getTitle() {
		return JPA.translate(title, title, getModel(), "button");
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Boolean getShowTitle() {
		return showTitle;
	}

	public void setShowTitle(Boolean showTitle) {
		this.showTitle = showTitle;
	}

	public List<Item> getItems() {
		if(items != null) {
			for (Item item : items) {
				item.setModel(getModel());
			}
		}
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getModel() {
		return model;
	}

}