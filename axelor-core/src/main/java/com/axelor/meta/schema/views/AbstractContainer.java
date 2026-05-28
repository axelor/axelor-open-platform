/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@XmlTransient
public abstract class AbstractContainer extends SimpleWidget {

  @XmlAttribute private Integer cols;

  @XmlAttribute private String colWidths;

  @XmlAttribute private String gap;

  @XmlAttribute private Integer itemSpan;

  public Integer getCols() {
    return cols;
  }

  public void setCols(Integer cols) {
    this.cols = cols;
  }

  public String getColWidths() {
    return colWidths;
  }

  public void setColWidths(String colWidths) {
    this.colWidths = colWidths;
  }

  public String getGap() {
    return gap;
  }

  public void setGap(String gap) {
    this.gap = gap;
  }

  public Integer getItemSpan() {
    return itemSpan;
  }

  public void setItemSpan(Integer itemSpan) {
    this.itemSpan = itemSpan;
  }
}
