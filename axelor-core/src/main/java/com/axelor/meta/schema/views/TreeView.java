/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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

import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Splitter;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeName("tree")
public class TreeView extends AbstractView {

  @XmlAttribute private Boolean showHeader;

  @XmlElement(name = "column")
  private List<TreeColumn> columns;

  @XmlElement(name = "node")
  private List<Node> nodes;

  public Boolean getShowHeader() {
    return showHeader;
  }

  public void setShowHeader(Boolean showHeader) {
    this.showHeader = showHeader;
  }

  public List<TreeColumn> getColumns() {
    return columns;
  }

  public void setColumns(List<TreeColumn> columns) {
    this.columns = columns;
  }

  public List<Node> getNodes() {
    return nodes;
  }

  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }

  @XmlType
  @JsonInclude(Include.NON_NULL)
  public static class TreeColumn extends BaseSearchField {}

  @XmlType
  @JsonInclude(Include.NON_NULL)
  public static class Node {

    @XmlAttribute private String model;

    @XmlAttribute private String parent;

    @XmlAttribute private String onClick;

    @XmlAttribute private String onMove;

    @XmlAttribute private Boolean draggable;

    @XmlAttribute private String domain;

    @XmlAttribute private String orderBy;

    @XmlElements({
      @XmlElement(name = "field", type = NodeField.class),
      @XmlElement(name = "button", type = Button.class)
    })
    private List<AbstractWidget> items;

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }

    public String getParent() {
      return parent;
    }

    public void setParent(String parent) {
      this.parent = parent;
    }

    public String getOnClick() {
      return onClick;
    }

    public void setOnClick(String onClick) {
      this.onClick = onClick;
    }

    public String getOnMove() {
      return onMove;
    }

    public void setOnMove(String onMove) {
      this.onMove = onMove;
    }

    public Boolean getDraggable() {
      return draggable;
    }

    public void setDraggable(Boolean draggable) {
      this.draggable = draggable;
    }

    public String getDomain() {
      return domain;
    }

    public void setDomain(String domain) {
      this.domain = domain;
    }

    public String getOrderBy() {
      return orderBy;
    }

    public void setOrderBy(String orderBy) {
      this.orderBy = orderBy;
    }

    public List<AbstractWidget> getItems() {
      if (items != null) {
        for (AbstractWidget item : items) {
          item.setModel(model);
        }
      }
      return items;
    }

    public void setItems(List<AbstractWidget> items) {
      this.items = items;
    }
  }

  @XmlType
  @JsonInclude(Include.NON_NULL)
  public static class NodeField extends PanelField {

    @XmlAttribute private String as;

    public String getAs() {
      return as;
    }

    public void setAs(String as) {
      this.as = as;
    }

    @Override
    public String getSelection() {
      final String selection = super.getSelection();
      if (selection != null) {
        return selection;
      }
      final Class<?> klass;
      try {
        klass = Class.forName(getModel());
      } catch (ClassNotFoundException e) {
        return null;
      }
      final Iterator<String> iter = Splitter.on(".").split(getName()).iterator();
      Mapper current = Mapper.of(klass);
      Property property = current.getProperty(iter.next());
      while (iter.hasNext()) {
        if (property == null || property.getTarget() == null) {
          return null;
        }
        current = Mapper.of(property.getTarget());
        property = current.getProperty(iter.next());
      }
      return property == null ? null : property.getSelection();
    }
  }
}
