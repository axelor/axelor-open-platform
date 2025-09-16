/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@JsonTypeName("dashboard")
public class Dashboard extends AbstractView implements ContainerView {

  @XmlElement(name = "field")
  @XmlElementWrapper(name = "search-fields")
  private List<BaseSearchField> searchFields;

  @XmlElements({@XmlElement(name = "dashlet", type = Dashlet.class)})
  private List<AbstractWidget> items;

  @XmlAttribute private String onInit;

  @Override
  public List<AbstractWidget> getItems() {
    return items;
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }

  public String getOnInit() {
    return onInit;
  }

  public void setOnInit(String onInit) {
    this.onInit = onInit;
  }

  public List<BaseSearchField> getSearchFields() {
    return searchFields;
  }

  public void setSearchFields(List<BaseSearchField> searchFields) {
    this.searchFields = searchFields;
  }
}
