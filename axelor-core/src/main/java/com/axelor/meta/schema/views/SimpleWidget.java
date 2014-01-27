/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.meta.schema.views;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlType
@XmlTransient
public abstract class SimpleWidget extends AbstractWidget {

	@XmlAttribute
	private String name;

	@XmlAttribute
	private String title;

	@XmlAttribute
	private String help;

	@XmlAttribute
	private Boolean showTitle;

	@XmlAttribute
	private Boolean hidden;

	@XmlAttribute
	private Boolean readonly;

	@XmlAttribute
	private String showIf;

	@XmlAttribute
	private String hideIf;

	@XmlAttribute
	private String readonlyIf;

	@XmlAttribute
	private Integer colSpan;

	@XmlAttribute
	private String css;

	@XmlAttribute
	private String width;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@JsonIgnore
	public String getDefaultTitle(){
		return title;
	}

	public String getTitle() {
		if (title == null || "".equals(title.trim())) {
			return title;
		}
		return JPA.translate(title);
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDefaultHelp(){
		return help;
	}

	public String getHelp() {
		return JPA.translate(help);
	}

	public void setHelp(String help) {
		this.help = help;
	}

	public Boolean getShowTitle() {
		return showTitle;
	}

	public void setShowTitle(Boolean showTitle) {
		this.showTitle = showTitle;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public Boolean getReadonly() {
		return readonly;
	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public String getShowIf() {
		return showIf;
	}

	public void setShowIf(String showIf) {
		this.showIf = showIf;
	}

	public String getHideIf() {
		return hideIf;
	}

	public void setHideIf(String hiddenIf) {
		this.hideIf = hiddenIf;
	}

	public String getReadonlyIf() {
		return readonlyIf;
	}

	public void setReadonlyIf(String readonlyIf) {
		this.readonlyIf = readonlyIf;
	}

	public Integer getColSpan() {
		return colSpan;
	}

	public void setColSpan(Integer colSpan) {
		this.colSpan = colSpan;
	}

	public String getCss() {
		return css;
	}

	public void setCss(String css) {
		this.css = css;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}
}
