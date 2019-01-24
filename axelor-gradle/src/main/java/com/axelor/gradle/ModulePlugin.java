/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

import com.axelor.common.VersionUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ModulePlugin implements Plugin<Project> {

  private final String version = VersionUtils.getVersion().version;

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(AxelorPlugin.class);

    // add core dependencies
    project.getDependencies().add("compile", "com.axelor:axelor-core:" + version);
    project.getDependencies().add("testCompile", "com.axelor:axelor-test:" + version);
  }
}
