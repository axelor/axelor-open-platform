/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.meta.domains;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.domains.Property;

@XmlType
public class Entity {

	@XmlElements({
		@XmlElement(name = "string", type = Property.class),
		@XmlElement(name = "boolean", type = Property.class),
        @XmlElement(name = "integer", type = Property.class),
        @XmlElement(name = "long", type = Property.class),
        @XmlElement(name = "decimal", type = Property.class),
        @XmlElement(name = "date", type = Property.class),
        @XmlElement(name = "time", type = Property.class),
        @XmlElement(name = "datetime", type = Property.class),
        @XmlElement(name = "one-to-one", type = Property.class),
        @XmlElement(name = "many-to-one", type = Property.class),
        @XmlElement(name = "one-to-many", type = Property.class),
        @XmlElement(name = "many-to-many", type = Property.class),
        @XmlElement(name = "binary", type = Property.class)
	})
	private List<Property> fields;

	@XmlAttribute
	private String name;

	@XmlAttribute
	private String table;

	@XmlAttribute
	private Boolean sequential;

	@XmlAttribute
	private String lang;

	@XmlAttribute
	private Boolean logUpdates;

	@XmlAttribute
	private Boolean hashAll;

	@XmlAttribute
	private Boolean cachable;

	@XmlAttribute
	private String indexes;

	public List<Property> getFields() {
		return fields;
	}

	public void setFields(List<Property> fields) {
		this.fields = fields;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public Boolean getSequential() {
		return sequential;
	}

	public void setSequential(Boolean sequential) {
		this.sequential = sequential;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public Boolean getLogUpdates() {
		return logUpdates;
	}

	public void setLogUpdates(Boolean logUpdates) {
		this.logUpdates = logUpdates;
	}

	public Boolean getHashAll() {
		return hashAll;
	}

	public void setHashAll(Boolean hashAll) {
		this.hashAll = hashAll;
	}

	public Boolean getCachable() {
		return cachable;
	}

	public void setCachable(Boolean cachable) {
		this.cachable = cachable;
	}

	public String getIndexes() {
		return indexes;
	}

	public void setIndexes(String indexes) {
		this.indexes = indexes;
	}
}
