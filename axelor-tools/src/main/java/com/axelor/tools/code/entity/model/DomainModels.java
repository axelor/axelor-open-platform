/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.tools.code.entity.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

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
