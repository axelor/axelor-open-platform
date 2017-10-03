/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.meta.schema.views;

import static com.axelor.common.StringUtils.isBlank;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.eclipse.persistence.oxm.annotations.XmlCDATA;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("field")
public class PanelField extends Field {

	@XmlElement
	@XmlCDATA
	private String viewer;

	@XmlElement
	private PanelEditor editor;

	public String getViewer() {
		return viewer;
	}

	public void setViewer(String viewer) {
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
}
