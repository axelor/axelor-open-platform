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

import static com.axelor.tools.code.entity.model.Utils.isTrue;
import static com.axelor.tools.code.entity.model.Utils.notBlank;

import com.axelor.common.StringUtils;
import com.axelor.tools.code.JavaAnnotation;
import com.axelor.tools.code.JavaCode;
import com.axelor.tools.code.JavaDoc;
import com.axelor.tools.code.JavaEnumConstant;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

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

  public JavaEnumConstant toJavaEnumConstant() {
    JavaEnumConstant constant = new JavaEnumConstant(name);
    if (notBlank(value)) {
      JavaCode arg = numeric ? new JavaCode("{0:l}", value) : new JavaCode("{0:s}", value);
      constant.arg(arg);
    }

    if (notBlank(title)) {
      constant.annotation(
          new JavaAnnotation("com.axelor.db.annotations.Widget").param("title", "{0:s}", title));
    }

    if (notBlank(help)) {
      constant.doc(new JavaDoc(StringUtils.stripIndent(help)));
    }

    return constant;
  }
}
