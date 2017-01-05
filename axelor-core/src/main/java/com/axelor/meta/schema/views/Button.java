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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
	
	@JsonGetter("title")
	public String getLocalizedTitle() {
		String title = getTitle();
		if (StringUtils.isBlank(title)) {
			return null;
		}
		return I18n.get(title);
	}

	public String getIcon() {
		return icon;
	}

	public String getIconHover() {
		return iconHover;
	}

	public String getLink() {
		return link;
	}

	@JsonGetter("prompt")
	public String getLocalizedPrompt() {
		return I18n.get(prompt);
	}

	@JsonIgnore
	public String getPrompt() {
		return prompt;
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
}
