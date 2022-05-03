/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

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
