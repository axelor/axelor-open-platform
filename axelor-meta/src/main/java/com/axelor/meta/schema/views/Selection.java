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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.axelor.db.JPA;
import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlType
public class Selection {

	@XmlAttribute
	private String name;

	@XmlElement(name = "option", required = true)
	private List<Selection.Option> options;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Selection.Option> getOptions() {
		return options;
	}

	public void setOptions(List<Selection.Option> options) {
		this.options = options;
	}

	@XmlType
	public static class Option {

		@XmlAttribute(required = true)
		private String value;

		@XmlValue
		private String title;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@JsonIgnore
		public String getDefaultTitle(){
			return title;
		}

		public String getTitle() {
			return JPA.translate(title, title, null, "select");
		}

		public void setTitle(String title) {
			this.title = title;
		}

	}

}
