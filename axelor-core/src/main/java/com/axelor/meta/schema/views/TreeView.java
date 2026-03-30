/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.axelor.common.StringUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.MetaStore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Splitter;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    @XmlAttribute(name = "x-json-model")
    private String jsonModel;

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

    public String getJsonModel() {
      return jsonModel;
    }

    public void setJsonModel(String jsonModel) {
      this.jsonModel = jsonModel;
    }

    public List<AbstractWidget> getItems() {
      if (items != null) {
        for (AbstractWidget item : items) {
          item.setModel(model);
          if (item instanceof NodeField) {
            ((NodeField) item).setNodeJsonModel(jsonModel);
          }
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

    private String nodeJsonModel;

    public String getAs() {
      return as;
    }

    public void setAs(String as) {
      this.as = as;
    }

    public void setNodeJsonModel(String jsonModel) {
      this.nodeJsonModel = jsonModel;
    }

    private Class<?> getNodeFieldTargetClass() {
      try {

        if (StringUtils.isBlank(nodeJsonModel)) {
          Mapper mapper = Mapper.of(Class.forName(this.getModel()));
          return mapper.getProperty(getName()).getTarget();
        }

        Class<?> modelClass = Class.forName(this.getModel());
        Mapper mapper = Mapper.of(modelClass);

        int dotIndex = getName().indexOf('.');
        if (dotIndex > 0) {
          String jsonField = getName().substring(0, dotIndex);
          String fieldName = getName().substring(dotIndex + 1);
          Property jsonProperty = mapper.getProperty(jsonField);

          if (jsonProperty != null && jsonProperty.isJson()) {
            Map<String, Object> jsonFields =
                StringUtils.notBlank(nodeJsonModel)
                    ? MetaStore.findJsonFields(nodeJsonModel)
                    : MetaStore.findJsonFields(modelClass.getName(), jsonField);

            if (jsonFields != null && jsonFields.containsKey(fieldName)) {
              Map<String, Object> attrs = (Map<String, Object>) jsonFields.get(fieldName);
              String target = (String) attrs.get("target");
              if (target != null) {
                return Class.forName(target);
              }
            }
          }
        }

        return null;

      } catch (ClassNotFoundException | NullPointerException e) {
        return null;
      }
    }

    @Override
    public String getTarget() {
      try {
        return getNodeFieldTargetClass().getName();
      } catch (NullPointerException e) {
        return null;
      }
    }

    @Override
    public String getTargetName() {
      String targetModel = getTarget();
      if (targetModel == null) return null;
      try {
        return Mapper.of(Class.forName(targetModel)).getNameField().getName();
      } catch (Exception e) {
        // ignore
      }
      return null;
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
