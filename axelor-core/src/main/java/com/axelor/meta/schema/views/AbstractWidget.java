/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.meta.schema.views;

import com.axelor.db.mapper.Mapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

@XmlType
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type", defaultImpl = Field.class)
@JsonInclude(Include.NON_NULL)
@JsonSubTypes({
  @Type(Field.class),
  @Type(Button.class),
  @Type(Break.class),
  @Type(Spacer.class),
  @Type(Separator.class),
  @Type(Label.class),
  @Type(Static.class),
  @Type(Help.class),
  @Type(Group.class),
  @Type(Notebook.class),
  @Type(Page.class),
  @Type(Portlet.class),
  @Type(Dashlet.class)
})
public abstract class AbstractWidget {

  @JsonIgnore
  @XmlAttribute(name = "if")
  private String conditionToCheck;

  @JsonIgnore
  @XmlAttribute(name = "if-module")
  private String moduleToCheck;

  @JsonIgnore @XmlAnyAttribute private Map<QName, String> otherAttributes;

  @XmlTransient @JsonIgnore private String model;

  public String getConditionToCheck() {
    return conditionToCheck;
  }

  public void setConditionToCheck(String conditionToCheck) {
    this.conditionToCheck = conditionToCheck;
  }

  public String getModuleToCheck() {
    return moduleToCheck;
  }

  public void setModuleToCheck(String moduleToCheck) {
    this.moduleToCheck = moduleToCheck;
  }

  public Map<QName, String> getOtherAttributes() {
    return otherAttributes;
  }

  public void setOtherAttributes(Map<QName, String> otherAttributes) {
    this.otherAttributes = otherAttributes;
  }

  @XmlTransient
  public Map<String, Object> getWidgetAttrs() {
    if (otherAttributes == null || otherAttributes.isEmpty()) {
      return null;
    }
    final Map<String, Object> attrs = Maps.newHashMap();
    for (QName qn : otherAttributes.keySet()) {
      String name = qn.getLocalPart();
      String value = otherAttributes.get(qn);
      if (name.startsWith("x-") || name.startsWith("data-")) {
        name = name.replaceFirst("^(x|data)-", "");
        name = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, name);
        attrs.put(name, value);
      }
    }
    if (attrs.containsKey("target") && !attrs.containsKey("targetName")) {
      try {
        Class<?> target = Class.forName(attrs.get("target").toString());
        String targetName = Mapper.of(target).getNameField().getName();
        attrs.put("targetName", targetName);
      } catch (Exception e) {
      }
    }
    return attrs;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }
}
