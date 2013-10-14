/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
