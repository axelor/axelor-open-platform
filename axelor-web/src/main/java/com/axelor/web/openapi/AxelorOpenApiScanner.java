/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.openapi;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.ObjectUtils;
import io.swagger.v3.jaxrs2.integration.JaxrsApplicationAndAnnotationScanner;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AxelorOpenApiScanner extends JaxrsApplicationAndAnnotationScanner {

  private static final Set<String> IGNORED = new HashSet<>();
  private List<String> excludeClasses =
      AppSettings.get().getList(AvailableAppSettings.APPLICATION_OPENAPI_SCAN_EXCLUDE_CLASSES);
  private List<String> excludePackages =
      AppSettings.get().getList(AvailableAppSettings.APPLICATION_OPENAPI_SCAN_EXCLUDE_PACKAGES);
  private List<String> includeClasses =
      AppSettings.get().getList(AvailableAppSettings.APPLICATION_OPENAPI_SCAN_CLASSES);
  private List<String> includePackages =
      AppSettings.get().getList(AvailableAppSettings.APPLICATION_OPENAPI_SCAN_PACKAGES);

  static {
    IGNORED.add("org.jboss.resteasy.core.AsynchronousDispatcher");
  }

  public AxelorOpenApiScanner() {
    super();
  }

  @Override
  public Set<Class<?>> classes() {
    Set<Class<?>> classes = super.classes();
    return classes.stream()
        .filter(aClass -> !this.isIgnored(aClass.getName()))
        .collect(Collectors.toSet());
  }

  /** */
  @Override
  protected boolean isIgnored(String classOrPackageName) {
    if (ObjectUtils.isEmpty(classOrPackageName)) {
      return true;
    }
    return super.isIgnored(classOrPackageName)
        || IGNORED.stream().anyMatch(classOrPackageName::startsWith)
        || shouldIgnoreClass(classOrPackageName);
  }

  private boolean shouldIgnoreClass(String className) {
    // if in application.openapi.scan.exclude.classes, class is excluded
    if (excludeClasses.contains(className)) {
      return true;
    }
    // if in application.openapi.scan.classes, class is included
    if (includeClasses.contains(className)) {
      return false;
    }

    // if package or any parent package in application.openapi.scan.exclude.packages unless a more
    // complete package or
    // parent package is named in application.openapi.scan.packages, class is excluded
    Optional<String> longestIncludeMatch = findLongestPackageMatch(className, includePackages);
    Optional<String> longestExcludeMatch = findLongestPackageMatch(className, excludePackages);

    if (longestExcludeMatch.isPresent()
        && (longestIncludeMatch.isEmpty()
            || longestIncludeMatch.get().length() <= longestExcludeMatch.get().length())) {
      // there is a more specific exclude
      return true;
    }

    // if package or any parent package in application.openapi.scan.packages, class is included
    if (longestIncludeMatch.isPresent()) return false;

    // if application.openapi.scan.classes and application.openapi.scan.packages are both
    // empty/undeclared, class is included
    return !(includeClasses.isEmpty() && includePackages.isEmpty());
  }

  private Optional<String> findLongestPackageMatch(String className, List<String> packageList) {
    return packageList.stream()
        .filter(className::startsWith)
        .max(Comparator.comparingInt(String::length));
  }
}
