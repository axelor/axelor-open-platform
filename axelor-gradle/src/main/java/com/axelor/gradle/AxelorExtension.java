/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.gradle;

import java.util.LinkedHashSet;
import java.util.Set;

public class AxelorExtension {

	public static final String EXTENSION_NAME = "axelor";

	private String title;

	private String description;

	private Boolean removable;

	private Set<String> install;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getRemovable() {
		return removable;
	}

	public void setRemovable(Boolean removable) {
		this.removable = removable;
	}

	public Set<String> getInstall() {
		return install;
	}

	public void setInstall(Set<String> install) {
		this.install = install;
	}

	public void title(String title) {
		this.title = title;
	}

	public void description(String description) {
		this.description = description;
	}

	public void removable(Boolean removable) {
		this.removable = removable;
	}

	public void install(String module) {
		if (install == null) {
			install = new LinkedHashSet<>();
		}
		install.add(module);
	}
}
