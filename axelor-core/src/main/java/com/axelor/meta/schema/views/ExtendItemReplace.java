/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.axelor.meta.schema.views.Menu.Divider;
import com.axelor.meta.schema.views.Menu.Item;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
public class ExtendItemReplace {

  @XmlElementWrapper
  @XmlElement(name = "button")
  private List<Button> toolbar;

  @XmlElementWrapper
  @XmlElement(name = "menu")
  private List<Menu> menubar;

  @XmlElement(name = "hilite")
  private List<Hilite> hilites;

  @XmlElements({
    @XmlElement(name = "field", type = PanelField.class),
    @XmlElement(name = "spacer", type = Spacer.class),
    @XmlElement(name = "static", type = Static.class),
    @XmlElement(name = "separator", type = Separator.class),
    @XmlElement(name = "label", type = Label.class),
    @XmlElement(name = "help", type = Help.class),
    @XmlElement(name = "button", type = Button.class),
    @XmlElement(name = "button-group", type = ButtonGroup.class),
    @XmlElement(name = "panel", type = Panel.class),
    @XmlElement(name = "panel-include", type = PanelInclude.class),
    @XmlElement(name = "panel-dashlet", type = Dashlet.class),
    @XmlElement(name = "panel-related", type = PanelRelated.class),
    @XmlElement(name = "panel-stack", type = PanelStack.class),
    @XmlElement(name = "panel-tabs", type = PanelTabs.class),
    @XmlElement(name = "panel-mail", type = PanelMail.class),
    @XmlElement(name = "menu", type = Menu.class),
    @XmlElement(name = "item", type = Item.class),
    @XmlElement(name = "divider", type = Divider.class)
  })
  private List<AbstractWidget> items;

  @XmlElement private ToolTip tooltip;

  @XmlElement private PanelViewer viewer;

  @XmlElement private PanelEditor editor;

  public List<Button> getToolbar() {
    return toolbar;
  }

  public void setToolbar(List<Button> toolbar) {
    this.toolbar = toolbar;
  }

  public List<Menu> getMenubar() {
    return menubar;
  }

  public void setMenubar(List<Menu> menubar) {
    this.menubar = menubar;
  }

  public List<Hilite> getHilites() {
    return hilites;
  }

  public void setHilites(List<Hilite> hilites) {
    this.hilites = hilites;
  }

  public List<AbstractWidget> getItems() {
    return items;
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }

  public ToolTip getTooltip() {
    return tooltip;
  }

  public void setTooltip(ToolTip tooltip) {
    this.tooltip = tooltip;
  }

  public PanelViewer getViewer() {
    return viewer;
  }

  public void setViewer(PanelViewer viewer) {
    this.viewer = viewer;
  }

  public PanelEditor getEditor() {
    return editor;
  }

  public void setEditor(PanelEditor editor) {
    this.editor = editor;
  }
}
