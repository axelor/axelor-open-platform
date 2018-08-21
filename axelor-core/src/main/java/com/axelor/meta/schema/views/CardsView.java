/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.eclipse.persistence.oxm.annotations.XmlCDATA;

@XmlType
@JsonTypeName("cards")
public class CardsView extends AbstractView {

  @XmlAttribute private String orderBy;

  @XmlAttribute private Boolean customSearch;

  @XmlAttribute private String freeSearch;

  @XmlElement(name = "field", type = Field.class)
  private List<AbstractWidget> items;

  @XmlElement(name = "hilite", type = Hilite.class)
  private List<Hilite> hilites;

  @XmlCDATA @XmlElement private String template;

  public String getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(String orderBy) {
    this.orderBy = orderBy;
  }

  public Boolean getCustomSearch() {
    return customSearch;
  }

  public void setCustomSearch(Boolean customSearch) {
    this.customSearch = customSearch;
  }

  public String getFreeSearch() {
    return freeSearch;
  }

  public void setFreeSearch(String freeSearch) {
    this.freeSearch = freeSearch;
  }

  public List<AbstractWidget> getItems() {
    if (items == null) {
      return items;
    }
    for (AbstractWidget item : items) {
      item.setModel(getModel());
    }
    return items;
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }

  public List<Hilite> getHilites() {
    return hilites;
  }

  public void setHilites(List<Hilite> hilites) {
    this.hilites = hilites;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }
}
