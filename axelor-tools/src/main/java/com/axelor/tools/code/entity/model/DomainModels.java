/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code.entity.model;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlType
@XmlRootElement(name = "domain-models")
public class DomainModels {

  @XmlElement(name = "module", required = true)
  private Namespace namespace;

  @XmlElement(name = "enum", type = EnumType.class)
  private List<EnumType> enums;

  @XmlElement(name = "entity", type = Entity.class)
  private List<Entity> entities;

  public Namespace getNamespace() {
    return namespace;
  }

  public void setNamespace(Namespace value) {
    this.namespace = value;
  }

  public List<EnumType> getEnums() {
    if (enums == null) {
      enums = new ArrayList<>();
    }
    return enums;
  }

  public void setEnums(List<EnumType> enums) {
    this.enums = enums;
  }

  public List<Entity> getEntities() {
    if (entities == null) {
      entities = new ArrayList<>();
    }
    return entities;
  }

  public void setEntities(List<Entity> entities) {
    this.entities = entities;
  }
}
