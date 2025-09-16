/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import org.eclipse.persistence.oxm.annotations.XmlCDATA;
import org.eclipse.persistence.oxm.annotations.XmlValueExtension;

@XmlType
@JsonTypeName("static")
public class Static extends SimpleWidget {

  @XmlCDATA @XmlValue @XmlValueExtension private String text;

  @JsonGetter("text")
  public String getLocaleText() {
    return text == null ? text : I18n.get(StringUtils.stripIndent(text).trim());
  }

  @JsonIgnore
  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
