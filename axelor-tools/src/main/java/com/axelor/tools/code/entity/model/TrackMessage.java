/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code.entity.model;

import static com.axelor.tools.code.entity.model.Utils.*;

import com.axelor.tools.code.JavaAnnotation;
import com.axelor.tools.code.JavaCode;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

@XmlType
public class TrackMessage {

  @XmlValue private String value;

  @XmlAttribute(name = "if", required = true)
  private String condition;

  @XmlAttribute(name = "on")
  private TrackEvent on;

  @XmlAttribute(name = "tag")
  private TrackTag tag;

  @XmlAttribute(name = "fields")
  private String fields;

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getCondition() {
    return condition;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  public TrackEvent getOn() {
    if (on == null) {
      return TrackEvent.ALWAYS;
    } else {
      return on;
    }
  }

  public void setOn(TrackEvent value) {
    this.on = value;
  }

  public TrackTag getTag() {
    return tag;
  }

  public void setTag(TrackTag value) {
    this.tag = value;
  }

  public String getFields() {
    return fields;
  }

  public void setFields(String value) {
    this.fields = value;
  }

  public JavaAnnotation toJavaAnnotation() {
    var annon =
        new JavaAnnotation("com.axelor.db.annotations.TrackMessage")
            .param("message", "{0:s}", value)
            .param("condition", "{0:s}", condition);

    if (tag != null) annon.param("tag", "{0:s}", tag.value());
    if (on != null) {
      annon.param("on", "{0:m}", "com.axelor.db.annotations.TrackEvent." + on);
    }

    annon.param("fields", list(fields), s -> new JavaCode("{0:s}", s));

    return annon;
  }
}
