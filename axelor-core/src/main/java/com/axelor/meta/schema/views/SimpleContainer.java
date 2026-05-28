/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@XmlTransient
public abstract class SimpleContainer extends AbstractContainer {

  @XmlElements({
    @XmlElement(name = "field", type = PanelField.class),
    @XmlElement(name = "spacer", type = Spacer.class),
    @XmlElement(name = "separator", type = Separator.class),
    @XmlElement(name = "label", type = Label.class),
    @XmlElement(name = "button", type = Button.class)
  })
  private List<AbstractWidget> items;

  public List<AbstractWidget> getItems() {
    if (items != null) {
      for (AbstractWidget abstractWidget : items) {
        abstractWidget.setModel(super.getModel());
      }
    }
    return items;
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }
}
