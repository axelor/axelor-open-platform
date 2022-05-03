/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlTransient
public abstract class SimpleContainer extends AbstractContainer {

  @XmlElements({
    @XmlElement(name = "field", type = Field.class),
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
