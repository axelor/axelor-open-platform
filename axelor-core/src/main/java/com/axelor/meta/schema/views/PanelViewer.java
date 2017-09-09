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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.eclipse.persistence.oxm.annotations.XmlCDATA;

import com.axelor.common.StringUtils;
import com.axelor.meta.MetaStore;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("viewer")
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonInclude(Include.NON_NULL)
public class PanelViewer {

	transient PanelField forField;

	@XmlAttribute
	private String depends;

	@XmlValue
	@XmlCDATA
	private String template;

	public String getDepends() {
		return depends;
	}

	public void setDepends(String depends) {
		this.depends = depends;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	@JsonGetter("fields")
	public Collection<?> getTargetFields() {
		if (forField == null || forField.getTarget() == null) {
			return null;
		}

		final Set<String> names = new HashSet<>();
		if (StringUtils.notBlank(depends)) {
			Collections.addAll(names, depends.trim().split("\\s*,\\s*"));
		}
		if (names.isEmpty()) {
			return null;
		}
		final Class<?> target;
		try {
			target = Class.forName(forField.getTarget());
		} catch (ClassNotFoundException e) {
			return null;
		}
		return (Collection<?>) MetaStore.findFields(target, names).get("fields");
	}
}
