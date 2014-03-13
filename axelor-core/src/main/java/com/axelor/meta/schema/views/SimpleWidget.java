/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
