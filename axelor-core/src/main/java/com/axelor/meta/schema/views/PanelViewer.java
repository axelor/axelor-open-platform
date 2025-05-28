/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.meta.schema.views;

import com.axelor.common.StringUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.MetaStore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import org.eclipse.persistence.oxm.annotations.XmlCDATA;

@XmlType
@JsonTypeName("viewer")
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonInclude(Include.NON_NULL)
public class PanelViewer {

  transient PanelField forField;

  @XmlAttribute private String depends;

  @XmlValue @XmlCDATA private String template;

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

  @JsonProperty(value = "fields", access = Access.READ_ONLY)
  public Collection<?> getTargetFields() {
    if (forField == null || forField.getTarget() == null) {
      return null;
    }

    final Set<String> names = new HashSet<>();
    if (StringUtils.notBlank(depends)) {
      for (String name : depends.split("\\s*,\\s*")) {
        if (StringUtils.notBlank(name)) {
          names.add(name);
        }
      }
    }

    final Class<?> target;
    try {
      target = Class.forName(forField.getTarget());
    } catch (ClassNotFoundException e) {
      return null;
    }

    if (names.isEmpty()) {
      Property nameField = Mapper.of(target).getNameField();
      if (nameField != null) {
        names.add(nameField.getName());
      }
    }

    return (Collection<?>) MetaStore.findFields(target, names).get("fields");
  }
}
