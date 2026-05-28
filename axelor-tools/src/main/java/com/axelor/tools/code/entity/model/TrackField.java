/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code.entity.model;

import static com.axelor.tools.code.entity.model.Utils.*;

import com.axelor.tools.code.JavaAnnotation;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

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
