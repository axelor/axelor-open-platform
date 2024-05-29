/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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

import static com.axelor.tools.code.entity.model.Utils.isTrue;
import static com.axelor.tools.code.entity.model.Utils.notBlank;

import com.axelor.common.StringUtils;
import com.axelor.tools.code.JavaAnnotation;
import com.axelor.tools.code.JavaCode;
import com.axelor.tools.code.JavaDoc;
import com.axelor.tools.code.JavaEnumConstant;
import java.util.HashMap;
import java.util.Map;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
public class EnumItem {

  @XmlAttribute(name = "name", required = true)
  private String name;

  @XmlAttribute(name = "title")
  private String title;

  @XmlAttribute(name = "value")
  private String value;

  @XmlAttribute(name = "help")
  private String help;

  @XmlAttribute(name = "data-description")
  private String description;

  @XmlAttribute(name = "icon")
  private String icon;

  @XmlAttribute(name = "hidden")
  private Boolean hidden;

  @XmlAttribute(name = "order")
  private Integer order;

  @XmlTransient private boolean numeric;

  void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    final EnumType et = ((EnumType) parent);
    this.numeric = isTrue(et.getNumeric());
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String value) {
    this.title = value;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getHelp() {
    return help;
  }

  public void setHelp(String value) {
    this.help = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public Boolean getHidden() {
    return hidden;
  }

  public void setHidden(Boolean hidden) {
    this.hidden = hidden;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }

  public JavaEnumConstant toJavaEnumConstant() {
    JavaEnumConstant constant = new JavaEnumConstant(name);
    if (notBlank(value)) {
      JavaCode arg = numeric ? new JavaCode("{0:l}", value) : new JavaCode("{0:s}", value);
      constant.arg(arg);
    }

    JavaAnnotation enumAnnotation = new JavaAnnotation("com.axelor.db.annotations.EnumWidget");
    Map<String, JavaCode> params = new HashMap<>();

    if (notBlank(title)) {
      params.put("title", new JavaCode("{0:s}", title));
    }
    if (notBlank(description)) {
      params.put("description", new JavaCode("{0:s}", description));
    }
    if (notBlank(icon)) {
      params.put("icon", new JavaCode("{0:s}", icon));
    }
    if (order != null) {
      params.put("order", new JavaCode("{0:l}", order));
    }
    if (hidden != null) {
      params.put("hidden", new JavaCode("{0:l}", hidden));
    }

    if (!params.isEmpty()) {
      params.forEach(enumAnnotation::param);
      constant.annotation(enumAnnotation);
    }

    if (notBlank(help)) {
      constant.doc(new JavaDoc(StringUtils.stripIndent(help)));
    }

    return constant;
  }
}
