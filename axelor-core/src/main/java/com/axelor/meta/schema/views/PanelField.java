/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import static com.axelor.common.StringUtils.isBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeName("field")
public class PanelField extends Field {

  @XmlElement private PanelViewer viewer;

  @XmlElement private PanelEditor editor;

  @XmlTransient @JsonIgnore private boolean fromEditor;

  public PanelViewer getViewer() {
    if (viewer != null) {
      viewer.forField = this;
    }
    return viewer;
  }

  public void setViewer(PanelViewer viewer) {
    this.viewer = viewer;
  }

  public PanelEditor getEditor() {
    if (editor != null) {
      editor.forField = this;
      editor.setModel(isBlank(getTarget()) ? getModel() : getTarget());
    }
    return editor;
  }

  public void setEditor(PanelEditor editor) {
    this.editor = editor;
  }

  public boolean isFromEditor() {
    return fromEditor;
  }

  void setFromEditor(boolean fromEditor) {
    this.fromEditor = fromEditor;
  }
}
