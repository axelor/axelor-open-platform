/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeName("button")
public class Button extends SimpleWidget {

  @XmlAttribute private String icon;

  @XmlAttribute private String iconHover;

  @XmlAttribute private String link;

  @XmlAttribute private String prompt;

  @XmlAttribute private String onClick;

  @XmlAttribute private String widget;

  @JsonGetter("title")
  public String getLocalizedTitle() {
    String title = getTitle();
    if (StringUtils.isBlank(title)) {
      return null;
    }
    return I18n.get(title);
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getIconHover() {
    return iconHover;
  }

  public void setIconHover(String iconHover) {
    this.iconHover = iconHover;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  @JsonGetter("prompt")
  public String getLocalizedPrompt() {
    return I18n.get(prompt);
  }

  @JsonIgnore
  public String getPrompt() {
    return prompt;
  }

  @JsonSetter
  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  public String getOnClick() {
    return onClick;
  }

  public void setOnClick(String onClick) {
    this.onClick = onClick;
  }

  public String getWidget() {
    return widget;
  }

  public void setWidget(String widget) {
    this.widget = widget;
  }
}
