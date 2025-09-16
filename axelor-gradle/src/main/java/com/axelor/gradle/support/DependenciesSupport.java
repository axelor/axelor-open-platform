/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
            files ->
                files
                    .getResolutionStrategy()
                    .eachDependency(
                        dependencyResolveDetails -> {
                          if (dependencyResolveDetails
                                  .getRequested()
                                  .getGroup()
                                  .equals("com.axelor")
                              && AxelorUtils.isCore(
                                  dependencyResolveDetails.getRequested().getName())) {
                            dependencyResolveDetails.useVersion(version);
                          }
                        }));
  }
}
