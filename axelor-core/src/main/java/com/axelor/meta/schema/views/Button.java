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
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("button")
public class Button extends SimpleWidget {

	@XmlAttribute
	private String icon;

	@XmlAttribute
	private String iconHover;

	@XmlAttribute
	private String link;

	@XmlAttribute
	private String prompt;

	@XmlAttribute
	private String onClick;

	public String getIcon() {
		return icon;
	}

	public String getIconHover() {
		return iconHover;
	}

	public String getLink() {
		return link;
	}

	public String getDefaultPrompt() {
		return prompt;
	}

	public String getPrompt() {
		return JPA.translate(prompt, prompt, super.getModel(), "button");
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public String getOnClick() {
		return onClick;
	}

	public void setOnClick(String onClick) {
		this.onClick = onClick;
	}

	@Override
	public String getHelp() {
		return JPA.translate(super.getDefaultHelp(), super.getDefaultHelp(), super.getModel(), "button");
	}

	@Override
	public String getTitle() {
		return JPA.translate(super.getDefaultTitle(), super.getDefaultTitle(), super.getModel(), "button");
	}
}
