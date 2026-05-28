/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeName("tooltip")
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonInclude(Include.NON_NULL)
public class ToolTip extends PanelViewer {

  @XmlAttribute private String call;

  public String getCall() {
    return call;
  }

  public void setCall(String call) {
    this.call = call;
  }
}
