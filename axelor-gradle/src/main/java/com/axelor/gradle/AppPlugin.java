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
package com.axelor.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import com.axelor.gradle.support.ScriptsSupport;
import com.axelor.gradle.support.TomcatSupport;
import com.axelor.gradle.support.WarSupport;

public class AppPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(ModulePlugin.class);
		project.getPlugins().apply(WarSupport.class);
		project.getPlugins().apply(ScriptsSupport.class);
		project.getPlugins().apply(TomcatSupport.class);
	}
}
