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
package com.axelor.tools.code.entity.model;

import static com.axelor.tools.code.entity.model.Utils.*;

import com.axelor.tools.code.JavaAnnotation;
import com.axelor.tools.code.JavaCode;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

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
