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
package com.axelor.meta.schema.views;

import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlTransient
public abstract class SimpleWidget extends AbstractWidget {

  @XmlAttribute private String name;

  @XmlAttribute private String title;

  @XmlAttribute private String help;

  @XmlAttribute private Boolean showTitle;

  @XmlAttribute private Boolean hidden;

  @XmlAttribute private Boolean readonly;

  @XmlAttribute private String showIf;

  @XmlAttribute private String hideIf;

  @XmlAttribute private String readonlyIf;

  @XmlAttribute private String depends;

  @XmlAttribute private String colSpan;

  @XmlAttribute private String colOffset;

  @XmlAttribute private String rowSpan;

  @XmlAttribute private String rowOffset;

  @XmlAttribute private String css;

  @XmlAttribute private String height;

  @XmlAttribute private String width;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @JsonGetter("autoTitle")
  public String getAutoTitle() {
    if (StringUtils.isBlank(title) && !StringUtils.isBlank(getName())) {
      String last = getName().substring(getName().lastIndexOf('.') + 1);
      return I18n.get(Inflector.getInstance().humanize(last));
    }
    return null;
  }

  @JsonGetter("title")
  public String getLocalizedTitle() {
    return I18n.get(title);
  }

  @JsonIgnore
  public String getTitle() {
    return title;
  }

  @JsonSetter
  public void setTitle(String title) {
    this.title = title;
  }

  @JsonGetter("help")
  public String getLocalizedHelp() {
    return I18n.get(help);
  }

  @JsonIgnore
  public String getHelp() {
    return help;
  }

  @JsonSetter
  public void setHelp(String help) {
    this.help = help;
  }

  public Boolean getShowTitle() {
    return showTitle;
  }

  public void setShowTitle(Boolean showTitle) {
    this.showTitle = showTitle;
  }

  public Boolean getHidden() {
    return hidden;
  }

  public void setHidden(Boolean hidden) {
    this.hidden = hidden;
  }

  public Boolean getReadonly() {
    return readonly;
  }

  public void setReadonly(Boolean readonly) {
    this.readonly = readonly;
  }

  public String getShowIf() {
    return showIf;
  }

  public void setShowIf(String showIf) {
    this.showIf = showIf;
  }

  public String getHideIf() {
    return hideIf;
  }

  public void setHideIf(String hiddenIf) {
    this.hideIf = hiddenIf;
  }

  public String getReadonlyIf() {
    return readonlyIf;
  }

  public void setReadonlyIf(String readonlyIf) {
    this.readonlyIf = readonlyIf;
  }

  public String getDepends() {
    return depends;
  }

  public void setDepends(String depends) {
    this.depends = depends;
  }

  public String getColSpan() {
    return colSpan;
  }

  public void setColSpan(String colSpan) {
    this.colSpan = colSpan;
  }

  public String getColOffset() {
    return colOffset;
  }

  public void setColOffset(String colOffset) {
    this.colOffset = colOffset;
  }

  public String getRowSpan() {
    return rowSpan;
  }

  public String getRowOffset() {
    return rowOffset;
  }

  public String getCss() {
    return css;
  }

  public void setCss(String css) {
    this.css = css;
  }

  public String getHeight() {
    return height;
  }

  public void setHeight(String height) {
    this.height = height;
  }

  public String getWidth() {
    return width;
  }

  public void setWidth(String width) {
    this.width = width;
  }
}
