/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@JsonTypeName("panel-tabs")
public class PanelTabs extends AbstractPanel {

  @XmlElements({
    @XmlElement(name = "panel", type = Panel.class),
    @XmlElement(name = "panel-related", type = PanelRelated.class),
    @XmlElement(name = "panel-dashlet", type = Dashlet.class),
    @XmlElement(name = "panel-include", type = PanelInclude.class)
  })
  private List<AbstractWidget> items;

  public List<AbstractWidget> getItems() {
    return process(items);
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }
}
