/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.openapi;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.ObjectUtils;
import com.axelor.meta.MetaScanner;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.swagger.v3.jaxrs2.integration.JaxrsApplicationAndAnnotationScanner;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Webhooks;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import jakarta.ws.rs.ApplicationPath;
import java.net.URL;
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
    // RESTEasy's internal dispatcher is not a real REST endpoint; ignore to avoid noise.
    IGNORED.add("org.jboss.resteasy.core.AsynchronousDispatcher");
  }

  public AxelorOpenApiScanner() {
    super();
  }

  @Override
  public Set<Class<?>> classes() {

    // Mirror parent's lazy-init guard so isAlwaysResolveAppPath() below is safe even when
    // the scanner is instantiated before setConfiguration() is called.
    if (openApiConfiguration == null) {
      openApiConfiguration = new SwaggerConfiguration();
    }

    // Narrow the ClassGraph scan to Axelor module JARs to avoid full-classpath OOM.
    // Directory-based classpath elements (build/classes, WEB-INF/classes) are not affected
    // by acceptJars() and remain accepted by default.
    ClassGraph graph = new ClassGraph().enableAllInfo();
    String[] jarNames = getAxelorModulesJarsNames();
    if (jarNames.length > 0) {
      graph.acceptJars(jarNames);
    }

    // Annotation pass — mirrors JaxrsAnnotationScanner#classes()
    final Set<Class<?>> classes;
    try (ScanResult scanResult = graph.scan()) {
      classes =
          new HashSet<>(
              scanResult
                  .getClassesWithAnnotation(jakarta.ws.rs.Path.class.getName())
                  .loadClasses());
      classes.addAll(
          new HashSet<>(
              scanResult
                  .getClassesWithAnnotation(OpenAPIDefinition.class.getName())
                  .loadClasses()));
      classes.addAll(
          new HashSet<>(
              scanResult.getClassesWithAnnotation(Webhooks.class.getName()).loadClasses()));
      if (Boolean.TRUE.equals(openApiConfiguration.isAlwaysResolveAppPath())) {
        classes.addAll(
            new HashSet<>(
                scanResult
                    .getClassesWithAnnotation(ApplicationPath.class.getName())
                    .loadClasses()));
      }
    }

    // Application pass — mirrors JaxrsApplicationAndAnnotationScanner#classes().
    // Pulls in resources contributed programmatically by a JAX-RS Application subclass via
    // getClasses() / getSingletons(). These bypass the JAR whitelist on purpose: the user
    // registered them explicitly, so location on the classpath is irrelevant.
    classes.addAll(addApplicationClasses());

    return classes.stream()
        .filter(aClass -> !this.isIgnored(aClass.getName()))
        .collect(Collectors.toSet());
  }

  /** Returns the list of Axelor module JAR file names */
  private String[] getAxelorModulesJarsNames() {
    return MetaScanner.findClassPath().stream()
        .map(URL::getFile)
        .filter(file -> file.endsWith(".jar"))
        .map(file -> file.substring(file.lastIndexOf('/') + 1))
        .toArray(String[]::new);
  }

  private Set<Class<?>> addApplicationClasses() {
    Set<Class<?>> output = new HashSet<>();
    if (application == null) {
      return output;
    }
    Set<Class<?>> appClasses = application.getClasses();
    if (appClasses != null) {
      output.addAll(appClasses);
    }
    Set<Object> singletons = application.getSingletons();
    if (singletons != null) {
      singletons.stream().map(Object::getClass).forEach(output::add);
    }
    return output;
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
