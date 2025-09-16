/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
public class CalendarEventHilite {

  @XmlAttribute(name = "if")
  private String condition;

  @XmlAttribute private String styles;

  public String getCondition() {
    return condition;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  public String getStyles() {
    return styles;
  }

  public void setStyles(String styles) {
    this.styles = styles;
  }
}
