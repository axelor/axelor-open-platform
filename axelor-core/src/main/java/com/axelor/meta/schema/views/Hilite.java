/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
public class Hilite {

  @XmlAttribute private String color;

  @XmlAttribute private String background;

  @XmlAttribute private Boolean strong;

  @XmlAttribute(name = "if")
  private String condition;

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getBackground() {
    return background;
  }

  public void setBackground(String background) {
    this.background = background;
  }

  public Boolean getStrong() {
    return strong;
  }

  public void setStrong(Boolean strong) {
    this.strong = strong;
  }

  public String getCss() {
    String css = "";
    if (color != null) {
      css += " hilite-" + color + "-text";
    }
    if (background != null) {
      css += " hilite-" + background;
    }
    if (Boolean.TRUE.equals(strong)) {
      css += " strong";
    }
    return css.trim();
  }

  public String getCondition() {
    return condition;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }
}
