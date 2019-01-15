/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.meta.schema.views;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeName("menu")
public class Menu extends SimpleWidget {

  @XmlType
  @JsonTypeName("menu-item")
  public static class Item extends MenuItem {

    @JsonIgnore private String model;

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }
  }

  @XmlType
  @JsonTypeName("menu-item-devider")
  public static class Devider extends Item {

    @Override
    public String getLocalizedTitle() {
      return null;
    }

    @Override
    public String getTitle() {
      return null;
    }
  }

  @XmlAttribute private String icon;

  @XmlElements({
    @XmlElement(name = "item", type = Item.class),
    @XmlElement(name = "menu", type = Menu.class),
    @XmlElement(name = "divider", type = Devider.class)
  })
  private List<AbstractWidget> items;

  @JsonIgnore private String model;

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public List<AbstractWidget> getItems() {
    if (items != null) {
      for (Object item : items) {
        if (item instanceof Item) {
          ((Item) item).setModel(getModel());
        }
        if (item instanceof Menu) {
          ((Menu) item).setModel(getModel());
        }
      }
    }
    return items;
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }
}
