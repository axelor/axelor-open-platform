/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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
