/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;
import org.eclipse.persistence.oxm.annotations.XmlCDATA;

@XmlType
@JsonTypeName("custom")
public class CustomView extends AbstractView implements ContainerView {

  @XmlElement(name = "field", type = PanelField.class)
  private List<AbstractWidget> items;

  @JsonIgnore
  @XmlElement(name = "dataset")
  private DataSet dataSet;

  @XmlCDATA @XmlElement private String template;

  @Override
  public List<AbstractWidget> getItems() {
    return items;
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }

  public DataSet getDataSet() {
    return dataSet;
  }

  public void setDataSet(DataSet dataSet) {
    this.dataSet = dataSet;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }
}
