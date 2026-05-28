/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeName("dashlet")
public class Dashlet extends AbstractPanel {

  @XmlAttribute private String action;

  @XmlAttribute private Boolean canSearch;

  @XmlAttribute private String canNew;

  @XmlAttribute private String canEdit;

  @XmlAttribute private String canDelete;

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public Boolean getCanSearch() {
    return canSearch;
  }

  public void setCanSearch(Boolean canSearch) {
    this.canSearch = canSearch;
  }

  public String getCanNew() {
    return canNew;
  }

  public void setCanNew(String canNew) {
    this.canNew = canNew;
  }

  public String getCanEdit() {
    return canEdit;
  }

  public void setCanEdit(String canEdit) {
    this.canEdit = canEdit;
  }

  public String getCanDelete() {
    return canDelete;
  }

  public void setCanDelete(String canDelete) {
    this.canDelete = canDelete;
  }
}
