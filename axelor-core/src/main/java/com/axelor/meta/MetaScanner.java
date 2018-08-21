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
package com.axelor.meta;

import com.axelor.common.ClassUtils;
import com.axelor.common.StringUtils;
import com.axelor.common.reflections.ClassFinder;
import com.axelor.common.reflections.Reflections;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** This class provides some utility methods to scan class path for resources/classes. */
public final class MetaScanner {

  private static final String MODULE_PROPERTIES = "module.properties";
  private static final String SCHEME_JAR = "jar";

  private static final List<String> BUILD_OUTPUT_PATHS =
      Arrays.asList(
          "bin/main",
          "out/production/classes",
          "out/production/resources",
          "build/resources/main",
          "build/classes/main",
          "build/classes/java/main",
          "build/classes/scala/main",
          "build/classes/kotlin/main",
          "build/classes/groovy/main");

  private MetaScanner() {}

  /**
   * Find module properties files within the class path.
   *
   * @return list of properties file urls
   */
  private static List<URL> findModuleFiles() {
    final List<URL> paths = new ArrayList<>();
    final ClassLoader loader = ClassUtils.getDefaultClassLoader();
    try {
      Enumeration<URL> found = loader.getResources(MODULE_PROPERTIES);
      while (found.hasMoreElements()) {
        paths.add(found.nextElement());
      }
    } catch (IOException e) {
    }
    return paths;
  }

  /**
   * Find the properties from the given module properties file.
   *
   * @param file the properties file url
   * @return module properties
   */
  private static Properties findProperties(URL file) {
    final Properties properties = new Properties();
    try (InputStream stream = file.openStream()) {
      properties.load(stream);
    } catch (IOException e) {
    }
    return properties;
  }

  /**
   * Find class path for the given module represented by the module file.
   *
   * <p>If the module file found in a jar, it will return jar url only. If the module file found in
   * a gradle build directory and a resource build directory exits, it will return list of classes
   * and resources build dir urls. Otherwise, the directory url only.
   *
   * @param moduleFile the module properties file
   * @return list of class path urls for the module
   */
  private static List<URL> findClassPath(URL moduleFile) {
    final List<URL> paths = new ArrayList<>();
    final String scheme = moduleFile.getProtocol();
    final boolean isJar = SCHEME_JAR.equals(scheme);

    String fileName = isJar ? moduleFile.getFile() : moduleFile.toString();
    fileName =
        fileName.substring(0, fileName.length() - MODULE_PROPERTIES.length() - (isJar ? 2 : 1));

    final Path file;
    try {
      file = Paths.get(new URI(fileName));
    } catch (URISyntaxException e) {
      // this should never happen
      throw new RuntimeException(e);
    }

    if (fileName.endsWith(".jar") || file.endsWith("WEB-INF/classes")) {
      try {
        paths.add(file.toUri().toURL());
      } catch (MalformedURLException e) {
        // this should never happen
      }
      return paths;
    }

    final Path base =
        BUILD_OUTPUT_PATHS
            .stream()
            .filter(p -> file.endsWith(p))
            .findFirst()
            .map(p -> p.replaceAll("[^/]+", ".."))
            .map(p -> file.resolve(p).normalize())
            .get();

    try {
      paths.add(file.toUri().toURL());
    } catch (MalformedURLException e1) {
      // this should never happen
    }

    final ClassLoader loader = ClassUtils.getContextClassLoader();
    if (loader instanceof URLClassLoader) {
      for (URL url : ((URLClassLoader) loader).getURLs()) {
        try {
          Path next = Paths.get(url.toURI());
          if (Files.isDirectory(next) && next.startsWith(base)) {
            paths.add(url);
          }
        } catch (URISyntaxException e) {
          // this should never happen
        }
      }
    } else {
      BUILD_OUTPUT_PATHS
          .stream()
          .map(base::resolve)
          .filter(Files::exists)
          .forEach(
              next -> {
                try {
                  paths.add(next.toUri().toURL());
                } catch (MalformedURLException e) {
                  // this should never happen
                }
              });
    }

    return paths;
  }

  /**
   * Find class path entries of modules excluding library paths.
   *
   * @return list of class path entry urls of the modules.
   */
  private static List<URL> findClassPath() {
    return findModuleFiles()
        .stream()
        .flatMap(file -> findClassPath(file).stream())
        .collect(Collectors.toList());
  }

  /**
   * Run scanner task within module only class path. This will greatly speed up scanning process.
   *
   * @param task the scanning task
   * @return scan result
   */
  private static <T> T findWithinModules(Supplier<T> task) {
    final List<URL> urls = findClassPath();
    return findWithinModules(urls.toArray(new URL[] {}), task);
  }

  /**
   * Run scanner task within the given module's class path.
   *
   * @param module the module to scan
   * @param task the scanning task
   * @return scan result
   */
  private static <T> T findWithin(String module, Supplier<T> task) {
    final List<URL> urls = new ArrayList<>();
    for (URL file : findModuleFiles()) {
      Properties info = findProperties(file);
      if (module.equals(info.getProperty("name"))) {
        urls.addAll(findClassPath(file));
        break;
      }
    }
    return findWithinModules(urls.toArray(new URL[] {}), task);
  }

  private static <T> T findWithinModules(URL[] paths, Supplier<T> task) {
    final ClassLoader context = ClassUtils.getContextClassLoader();
    final ClassLoader wrapper =
        new URLClassLoader(paths, null) {

          @Override
          public Class<?> loadClass(String name) throws ClassNotFoundException {
            return context.loadClass(name);
          }
        };
    try {
      ClassUtils.setContextClassLoader(wrapper);
      return task.get();
    } finally {
      ClassUtils.setContextClassLoader(context);
    }
  }

  /**
   * Find module properties within the class paths.
   *
   * @return list of module properties
   */
  public static List<Properties> findModuleProperties() {
    return findModuleFiles()
        .stream()
        .map(file -> findProperties(file))
        .filter(p -> StringUtils.notBlank(p.getProperty("name")))
        .collect(Collectors.toList());
  }

  /**
   * Find all resources matched by the given pattern.
   *
   * @param pattern the resource name pattern to match
   * @return list of resources matched
   */
  public static List<URL> findAll(String pattern) {
    return findWithinModules(() -> Reflections.findResources().byName(pattern).find());
  }

  /**
   * Find all resources within a directory of the given module matching the given pattern.
   *
   * @param module the module name
   * @param directory the resource directory name
   * @param pattern the resource name pattern to match
   * @return list of resources matched
   */
  public static List<URL> findAll(String module, String directory, String pattern) {
    final String namePattern = directory + "(/|\\\\)" + pattern;
    return findWithin(module, () -> Reflections.findResources().byName(namePattern).find());
  }

  /**
   * Delegates to {@link Reflections#findSubTypesOf(Class)} but searches within module paths only.
   *
   * @see Reflections#findSubTypesOf(Class)
   */
  public static <T> ClassFinder<T> findSubTypesOf(Class<T> type) {
    return findWithinModules(() -> Reflections.findSubTypesOf(type));
  }

  /**
   * Same as {@link #findSubTypesOf(Class)} but searches only within given module.
   *
   * @see #findSubTypesOf(Class)
   */
  public static <T> ClassFinder<T> findSubTypesOf(String module, Class<T> type) {
    return findWithin(module, () -> Reflections.findSubTypesOf(type));
  }
}
