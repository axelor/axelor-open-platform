/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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

import static com.axelor.tools.code.entity.model.Utils.*;

import com.axelor.tools.code.JavaAnnotation;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class TrackField {

  @XmlAttribute(name = "name", required = true)
  private String name;

  @XmlAttribute(name = "if")
  private String condition;

  @XmlAttribute(name = "on")
  private TrackEvent on;

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getCondition() {
    return condition;
  }

  public TrackEvent getOn() {
    return on;
  }

  public void setOn(TrackEvent value) {
    this.on = value;
  }

  public JavaAnnotation toJavaAnnotation() {
    JavaAnnotation a =
        new JavaAnnotation("com.axelor.db.annotations.TrackField").param("name", "{0:s}", name);

    if (notBlank(condition)) a.param("condition", "{0:s}", condition);
    if (on != null) a.param("on", "{0:m}", "com.axelor.db.annotations.TrackEvent." + on);

    return a;
  }
}
