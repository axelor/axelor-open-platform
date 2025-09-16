/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlType
public abstract class AbstractPanel extends AbstractContainer {

  @XmlAttribute private Boolean showFrame;

  @XmlAttribute private Boolean sidebar;

  @XmlAttribute private Boolean stacked;

  @XmlAttribute private Boolean attached;

  @XmlAttribute private String onTabSelect;

  public Boolean getShowFrame() {
    return showFrame;
  }

  public void setShowFrame(Boolean showFrame) {
    this.showFrame = showFrame;
  }

  public Boolean getSidebar() {
    return sidebar;
  }

  public void setSidebar(Boolean sidebar) {
    this.sidebar = sidebar;
  }

  public Boolean getStacked() {
    return stacked;
  }

  public void setStacked(Boolean stacked) {
    this.stacked = stacked;
  }

  public Boolean getAttached() {
    return attached;
  }

  public void setAttached(Boolean attached) {
    this.attached = attached;
  }

  public String getOnTabSelect() {
    return onTabSelect;
  }

  public void setOnTabSelect(String onTabSelect) {
    this.onTabSelect = onTabSelect;
  }

  protected List<AbstractWidget> process(List<AbstractWidget> items) {
    if (items == null) {
      items = new ArrayList<>();
    }
    for (AbstractWidget item : items) {
      item.setModel(getModel());
    }
    return items;
  }
}
