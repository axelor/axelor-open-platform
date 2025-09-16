/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;
import org.eclipse.persistence.oxm.annotations.XmlCDATA;

@XmlType
@JsonTypeName("cards")
public class CardsView extends AbstractView implements ContainerView {

  @XmlAttribute private String orderBy;

  @XmlAttribute private Boolean customSearch;

  @XmlAttribute private String freeSearch;

  @XmlElementWrapper
  @XmlElement(name = "button")
  private List<Button> toolbar;

  @XmlElementWrapper
  @XmlElement(name = "menu")
  private List<Menu> menubar;

  @XmlElement(name = "field", type = PanelField.class)
  private List<AbstractWidget> items;

  @XmlElement(name = "hilite", type = Hilite.class)
  private List<Hilite> hilites;

  @XmlCDATA @XmlElement private String template;

  @XmlAttribute private Boolean canNew;

  @XmlAttribute private Boolean canEdit;

  @XmlAttribute private Boolean canDelete;

  @XmlAttribute(name = "edit-window")
  private String editWindow;

  @XmlAttribute private String onDelete;

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

  public List<Button> getToolbar() {
    if (toolbar != null) {
      for (Button button : toolbar) {
        button.setModel(this.getModel());
      }
    }
    return toolbar;
  }

  public void setToolbar(List<Button> toolbar) {
    this.toolbar = toolbar;
  }

  public List<Menu> getMenubar() {
    if (menubar != null) {
      for (Menu menu : menubar) {
        menu.setModel(this.getModel());
      }
    }
    return menubar;
  }

  public void setMenubar(List<Menu> menubar) {
    this.menubar = menubar;
  }

  @Override
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

  public Boolean getCanNew() {
    return canNew;
  }

  public void setCanNew(Boolean canNew) {
    this.canNew = canNew;
  }

  public Boolean getCanEdit() {
    return canEdit;
  }

  public void setCanEdit(Boolean canEdit) {
    this.canEdit = canEdit;
  }

  public Boolean getCanDelete() {
    return canDelete;
  }

  public void setCanDelete(Boolean canDelete) {
    this.canDelete = canDelete;
  }

  public String getEditWindow() {
    return editWindow;
  }

  public void setEditWindow(String editWindow) {
    this.editWindow = editWindow;
  }

  public String getOnDelete() {
    return onDelete;
  }

  public void setOnDelete(String onDelete) {
    this.onDelete = onDelete;
  }
}
