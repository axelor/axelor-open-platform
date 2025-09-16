/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code.entity.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
public class EntityListener {

  @XmlAttribute(name = "class", required = true)
  private String clazz;

  public String getClazz() {
    return clazz;
  }

  public void setClazz(String value) {
    this.clazz = value;
  }
}
