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

import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.meta.MetaStore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@XmlType
@JsonTypeName("editor")
public class PanelEditor extends AbstractPanel {

  transient PanelField forField;
  transient List<Object> targetFields;

  @XmlTransient @JsonIgnore private boolean fromEditorProcessed;

  @XmlAttribute private String layout;

  @XmlAttribute(name = "x-viewer")
  private Boolean viewer;

  @XmlAttribute(name = "x-show-on-new")
  private Boolean showOnNew;

  @XmlAttribute private String onNew;

  @XmlElements({
    @XmlElement(name = "field", type = PanelField.class),
    @XmlElement(name = "button", type = Button.class),
    @XmlElement(name = "spacer", type = Spacer.class),
    @XmlElement(name = "separator", type = Separator.class),
    @XmlElement(name = "label", type = Label.class),
    @XmlElement(name = "panel", type = Panel.class)
  })
  private List<AbstractWidget> items;

  public String getLayout() {
    return layout;
  }

  public void setLayout(String layout) {
    this.layout = layout;
  }

  public Boolean getViewer() {
    return viewer;
  }

  public void setViewer(Boolean viewer) {
    this.viewer = viewer;
  }

  public Boolean getShowOnNew() {
    return showOnNew;
  }

  public void setShowOnNew(Boolean showOnNew) {
    this.showOnNew = showOnNew;
  }

  public String getOnNew() {
    return onNew;
  }

  public void setOnNew(String onNew) {
    this.onNew = onNew;
  }

  public List<AbstractWidget> getItems() {
    // process target fields
    getTargetFields();

    if (!fromEditorProcessed) {
      processFromEditor();
      fromEditorProcessed = true;
    }

    return process(items);
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }

  private Set<String> findFields(AbstractWidget widget) {

    final Set<String> all = new HashSet<>();

    if (widget instanceof SimpleWidget simpleWidget) {
      String depends = simpleWidget.getDepends();
      if (StringUtils.notBlank(depends)) {
        Collections.addAll(all, depends.trim().split("\\s*,\\s*"));
      }
    }

    if (widget instanceof Field field) {
      all.add(field.getName());
      // include related field for ref-select widget
      String relatedAttr = field.getRelated();
      if (StringUtils.notBlank(relatedAttr)) {
        all.add(relatedAttr);
      }
      return all;
    }

    if (widget instanceof PanelEditor panelEditor) {
      for (AbstractWidget item : panelEditor.getItems()) {
        all.addAll(findFields(item));
      }
    } else if (widget instanceof Panel panel) {
      for (AbstractWidget item : panel.getItems()) {
        all.addAll(findFields(item));
      }
    }

    return all;
  }

  @JsonProperty(value = "fields", access = Access.READ_ONLY)
  public List<Object> getTargetFields() {
    if (targetFields != null || items == null || forField == null || forField.getTarget() == null) {
      return targetFields;
    }
    this.targetFields = new ArrayList<>();
    final Class<?> target;
    try {
      target = Class.forName(forField.getTarget());
    } catch (ClassNotFoundException e) {
      return null;
    }
    final Map<String, Object> fields = MetaStore.findFields(target, findFields(this));
    this.targetFields.addAll((Collection<?>) fields.get("fields"));

    return targetFields;
  }

  /** Sets flags in child fields so that we know those fields are within an editor. */
  protected void processFromEditor() {
    if (ObjectUtils.isEmpty(items)) {
      return;
    }

    final Queue<AbstractWidget> widgets = new ArrayDeque<>();
    widgets.addAll(items);

    while (!widgets.isEmpty()) {
      final AbstractWidget widget = widgets.remove();
      if (widget instanceof PanelField panelField) {
        panelField.setFromEditor(true);
      } else if (widget instanceof Panel panel) {
        widgets.addAll(panel.getItems());
      }
    }
  }
}
