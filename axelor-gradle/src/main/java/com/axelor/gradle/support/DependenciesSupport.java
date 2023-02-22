/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.gradle.support;

import com.axelor.common.VersionUtils;
import com.axelor.gradle.AxelorUtils;
import org.gradle.api.Project;

/**
 * The plugin manage the version of AOP dependencies used.
 *
 * <p>All AOP core dependencies (direct or transitive) will use the version defined in the main
 * project. Plugin can be applied on the root project :
 *
 * <pre>
 * apply plugin: com.axelor.gradle.support.DependenciesSupport
 * </pre>
 */
public class DependenciesSupport extends AbstractSupport {

  private final String version = VersionUtils.getVersion().version;

  @Override
  public void apply(Project project) {
    project
        .getConfigurations()
        .forEach(
            files -> {
              files
                  .getResolutionStrategy()
                  .eachDependency(
                      dependencyResolveDetails -> {
                        if (dependencyResolveDetails.getRequested().getGroup().equals("com.axelor")
                            && AxelorUtils.isCore(
                                dependencyResolveDetails.getRequested().getName())) {
                          dependencyResolveDetails.useVersion(version);
                        }
                      });
            });
  }
}
