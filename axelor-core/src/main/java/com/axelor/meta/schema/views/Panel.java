/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@JsonTypeName("panel")
public class Panel extends AbstractPanel {

  @XmlAttribute private Boolean canCollapse;

  @XmlAttribute private String collapseIf;

  @XmlAttribute private String icon;

  @XmlAttribute(name = "icon-background")
  private String iconBackground;

  @XmlElement private Menu menu;

  @XmlElements({
    @XmlElement(name = "field", type = PanelField.class),
    @XmlElement(name = "spacer", type = Spacer.class),
    @XmlElement(name = "label", type = Label.class),
    @XmlElement(name = "static", type = Static.class),
    @XmlElement(name = "separator", type = Separator.class),
    @XmlElement(name = "help", type = Help.class),
    @XmlElement(name = "button", type = Button.class),
    @XmlElement(name = "button-group", type = ButtonGroup.class),
    @XmlElement(name = "panel", type = Panel.class),
    @XmlElement(name = "panel-related", type = PanelRelated.class),
    @XmlElement(name = "panel-dashlet", type = Dashlet.class),
    @XmlElement(name = "panel-include", type = PanelInclude.class)
  })
  private List<AbstractWidget> items;

  public Boolean getCanCollapse() {
    return canCollapse;
  }

  public void setCanCollapse(Boolean canCollapse) {
    this.canCollapse = canCollapse;
  }

  public String getCollapseIf() {
    return collapseIf;
  }

  public void setCollapseIf(String collapseIf) {
    this.collapseIf = collapseIf;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getIconBackground() {
    return iconBackground;
  }

  public void setIconBackground(String iconBackground) {
    this.iconBackground = iconBackground;
  }

  public Menu getMenu() {
    return menu;
  }

  public void setMenu(Menu menu) {
    this.menu = menu;
  }

  public List<AbstractWidget> getItems() {
    return process(items);
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }
}
