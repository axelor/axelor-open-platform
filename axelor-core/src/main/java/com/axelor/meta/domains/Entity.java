/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
