/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.axelor.meta.loader.XMLViews;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeName("include")
public class PanelInclude extends AbstractWidget {

  @XmlAttribute(name = "view")
  private String name;

  @XmlAttribute(name = "from")
  private String module;

  @XmlTransient @JsonIgnore private transient AbstractView owner;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getModule() {
    return module;
  }

  public void setModule(String module) {
    this.module = module;
  }

  public void setOwner(AbstractView owner) {
    this.owner = owner;
  }

  @JsonInclude
  public AbstractView getView() {
    AbstractView view = XMLViews.findView(name, null, null, module);
    if (view == owner) {
      return null;
    }
    view.setOwner(owner);
    return view;
  }
}
