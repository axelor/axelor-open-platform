/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.actions;

import com.axelor.meta.ActionHandler;
import com.google.common.base.Strings;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlType
public class ActionAttrs extends Action {

  @XmlType
  public static class Attribute extends Element {

    @XmlAttribute(name = "for")
    private String fieldName;

    public String getFieldName() {
      return fieldName;
    }

    public void setFieldName(String fieldName) {
      this.fieldName = fieldName;
    }
  }

  @XmlElement(name = "attribute", type = Attribute.class)
  private List<Attribute> attributes;

  public List<Attribute> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<Attribute> attributes) {
    this.attributes = attributes;
  }

  @Override
  @SuppressWarnings("all")
  protected Object evaluate(ActionHandler handler) {

    Map<String, Object> map = new HashMap<>();
    for (Attribute attribute : attributes) {
      if (!attribute.test(handler) || Strings.isNullOrEmpty(attribute.getFieldName())) continue;
      for (String field : attribute.fieldName.split(",")) {
        if (Strings.isNullOrEmpty(field)) {
          continue;
        }
        field = field.trim();
        Map<String, Object> attrs = (Map) map.get(field);
        if (attrs == null) {
          attrs = new HashMap<>();
          map.put(field, attrs);
        }

        String name = attribute.getName();
        Object value = null;
        if (name.matches("readonly|required|hidden|collapse|refresh|focus|active")) {
          value = Action.test(handler, attribute.getExpression());
        } else {
          value = handler.evaluate(attribute.getExpression());
        }
        attrs.put(attribute.getName(), value);
      }
    }
    return map;
  }

  @Override
  protected Object wrapper(Object value) {
    if (value == null) {
      return null;
    }
    final Map<String, Object> result = new HashMap<>();
    result.put("attrs", value);
    return result;
  }
}
