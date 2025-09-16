/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.axelor.i18n.I18n;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

@XmlType
public class Selection {

  @XmlAttribute private String name;

  @XmlAttribute(name = "id")
  private String xmlId;

  @XmlElement(name = "option", required = true)
  private List<Selection.Option> options;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getXmlId() {
    return xmlId;
  }

  public void setXmlId(String xmlId) {
    this.xmlId = xmlId;
  }

  public List<Selection.Option> getOptions() {
    return options;
  }

  public void setOptions(List<Selection.Option> options) {
    this.options = options;
  }

  @XmlType
  public static class Option {

    @XmlAttribute(required = true)
    private String value;

    @XmlValue private String title;

    @XmlAttribute private String icon;

    @XmlAttribute private String color;

    @XmlAttribute private Integer order;

    @XmlAttribute private Boolean hidden;

    @JsonIgnore @XmlAnyAttribute private Map<QName, String> dataAttributes;

    @XmlTransient private Map<String, Object> data;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    @JsonGetter("title")
    public String getLocalizedTitle() {
      return I18n.get(title);
    }

    @JsonIgnore
    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getIcon() {
      return icon;
    }

    public void setIcon(String icon) {
      this.icon = icon;
    }

    public String getColor() {
      return color;
    }

    public void setColor(String color) {
      this.color = color;
    }

    public Integer getOrder() {
      return order;
    }

    public void setOrder(Integer order) {
      this.order = order;
    }

    public Boolean getHidden() {
      return hidden;
    }

    public void setHidden(Boolean hidden) {
      this.hidden = hidden;
    }

    public Map<QName, String> getDataAttributes() {
      return dataAttributes;
    }

    @JsonGetter
    public Map<String, Object> getData() {
      if (data == null || data.isEmpty()) {
        return data;
      }
      Map<String, Object> _data = new HashMap<>();
      for (Map.Entry<String, Object> entry : data.entrySet()) {
        Object val = entry.getValue();
        if ("description".equals(entry.getKey()) && val != null) {
          val = I18n.get(val.toString());
        }
        _data.put(entry.getKey(), val);
      }
      return _data;
    }

    public void setData(Map<String, Object> data) {
      this.data = data;
    }
  }
}
