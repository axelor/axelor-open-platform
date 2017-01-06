/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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

import java.util.List;

import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaFieldCustom;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@XmlType
@JsonInclude(Include.NON_DEFAULT)
public class JsonField {

	private String name;

	private String type;

	private String title;

	private String defaultValue;

	private String help;

	private String selection;

	private Integer minSize;

	private Integer maxSize;

	private String regex;

	private Boolean required;

	private Boolean hidden;

	private String showIf;

	private String hideIf;

	private String requiredIf;

	public JsonField(MetaFieldCustom field) {
		this.name = field.getName();
		this.type = field.getType();
		this.title = field.getTitle();
		this.defaultValue = field.getDefaultValue();
		this.selection = field.getSelection();
		this.help = field.getHelp();
		this.minSize = field.getMinSize();
		this.maxSize = field.getMaxSize();
		this.required = field.getRequired();
		this.hidden = field.getHidden();
		this.showIf = field.getShowIf();
		this.hideIf = field.getHideIf();
		this.requiredIf = field.getRequiredIf();
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getTitle() {
		return title;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public String getSelection() {
		return selection;
	}
	
	public List<?> getSelectionList() {
		return MetaStore.getSelectionList(getSelection());
	}

	public String getHelp() {
		return help;
	}

	public Integer getMinSize() {
		return minSize;
	}

	public Integer getMaxSize() {
		return maxSize;
	}

	public String getRegex() {
		return regex;
	}

	public Boolean getRequired() {
		return required;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public String getShowIf() {
		return showIf;
	}

	public String getHideIf() {
		return hideIf;
	}

	public String getRequiredIf() {
		return requiredIf;
	}
}
