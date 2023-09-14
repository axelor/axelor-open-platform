/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeName("form")
public class FormView extends AbstractView implements ContainerView, ExtendableView {

  @XmlAttribute private String onLoad;

  @XmlAttribute private String onSave;

  @XmlAttribute private String onNew;

  @XmlAttribute private String readonlyIf;

  @XmlAttribute private String canNew;

  @XmlAttribute private String canEdit;

  @XmlAttribute private String canSave;

  @XmlAttribute private String canDelete;

  @XmlAttribute private String canArchive;

  @XmlAttribute private String canCopy;

  @XmlAttribute private String canAttach;

  @XmlAttribute private Integer cols;

  @XmlAttribute private String colWidths;

  @XmlAttribute private String gap;

  @XmlAttribute private Integer itemSpan;

  @XmlElementWrapper
  @XmlElement(name = "button")
  private List<Button> toolbar;

  @XmlElementWrapper
  @XmlElement(name = "menu")
  private List<Menu> menubar;

  @XmlElements({
    @XmlElement(name = "help", type = Help.class),
    @XmlElement(name = "panel", type = Panel.class),
    @XmlElement(name = "panel-include", type = PanelInclude.class),
    @XmlElement(name = "panel-dashlet", type = Dashlet.class),
    @XmlElement(name = "panel-related", type = PanelRelated.class),
    @XmlElement(name = "panel-stack", type = PanelStack.class),
    @XmlElement(name = "panel-tabs", type = PanelTabs.class),
    @XmlElement(name = "panel-mail", type = PanelMail.class)
  })
  private List<AbstractWidget> items;

  @XmlElement(name = "extend")
  private List<Extend> extendItems;

  public String getOnLoad() {
    return onLoad;
  }

  public void setOnLoad(String onLoad) {
    this.onLoad = onLoad;
  }

  public String getOnSave() {
    return onSave;
  }

  public void setOnSave(String onSave) {
    this.onSave = onSave;
  }

  public String getOnNew() {
    return onNew;
  }

  public void setOnNew(String onNew) {
    this.onNew = onNew;
  }

  public String getReadonlyIf() {
    return readonlyIf;
  }

  public void setReadonlyIf(String readonlyIf) {
    this.readonlyIf = readonlyIf;
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

  public String getCanSave() {
    return canSave;
  }

  public void setCanSave(String canSave) {
    this.canSave = canSave;
  }

  public String getCanDelete() {
    return canDelete;
  }

  public void setCanDelete(String canDelete) {
    this.canDelete = canDelete;
  }

  public String getCanArchive() {
    return canArchive;
  }

  public void setCanArchive(String canArchive) {
    this.canArchive = canArchive;
  }

  public String getCanCopy() {
    return canCopy;
  }

  public void setCanCopy(String canCopy) {
    this.canCopy = canCopy;
  }

  public String getCanAttach() {
    return canAttach;
  }

  public void setCanAttach(String canAttach) {
    this.canAttach = canAttach;
  }

  public Integer getCols() {
    return cols;
  }

  public void setCols(Integer cols) {
    this.cols = cols;
  }

  public String getColWidths() {
    return colWidths;
  }

  public void setColWidths(String colWidths) {
    this.colWidths = colWidths;
  }

  public String getGap() {
    return gap;
  }

  public void setGap(String gap) {
    this.gap = gap;
  }

  public Integer getItemSpan() {
    return itemSpan;
  }

  public void setItemSpan(Integer itemSpan) {
    this.itemSpan = itemSpan;
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
  @JsonProperty("items")
  public List<AbstractWidget> getItems() {
    if (items == null) {
      return Collections.emptyList();
    }
    for (AbstractWidget item : items) {
      item.setModel(super.getModel());
      if (item instanceof PanelInclude) {
        ((PanelInclude) item).setOwner(this);
      }
    }
    return items;
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }

  @Override
  public List<AbstractWidget> getExtraItems() {
    final List<AbstractWidget> extraItems = new ArrayList<>();

    Optional.ofNullable(getToolbar()).ifPresent(extraItems::addAll);
    Optional.ofNullable(getMenubar())
        .ifPresent(
            m ->
                m.stream()
                    .map(Menu::getItems)
                    .filter(Objects::nonNull)
                    .forEach(extraItems::addAll));

    return Collections.unmodifiableList(extraItems);
  }

  @Override
  public List<Extend> getExtends() {
    return extendItems;
  }

  public void setExtends(List<Extend> extendItems) {
    this.extendItems = extendItems;
  }
}
