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
package com.axelor.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/** Provides class utility methods. */
public final class ClassUtils {

  private static final String PROXY_CLASS_SEPARATOR = "$$";

  private static final String CLASSPATH_URL_PREFIX = "classpath:";

  private static final String PATH_SEPARATOR = "/";

  private static final String PACKAGE_SEPARATOR = ".";

  private static final String CLASS_FILE_SUFFIX = ".class";

  private ClassUtils() {}

  /**
   * Returns the context ClassLoader for this Thread.
   *
   * @return the context ClassLoader for this Thread, or null indicating the system class loader.
   * @see Thread#getContextClassLoader()
   */
  public static ClassLoader getContextClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  /**
   * Override the context ClassLoader of this Thread.
   *
   * @param classLoader the ClassLoader to override.
   * @return the previous context ClassLoader, or null if ClassLoader is not overriden.
   */
  public static ClassLoader setContextClassLoader(ClassLoader classLoader) {
    final ClassLoader contextLoader = getContextClassLoader();
    if (classLoader == null || classLoader.equals(contextLoader)) {
      return null;
    }
    Thread.currentThread().setContextClassLoader(classLoader);
    return contextLoader;
  }

  /**
   * Returns the default ClassLoader.
   *
   * <p>Generally, it is current Thread context ClassLoader. If not available, it will be the
   * ClassLoader that loaded the {@link ClassUtils}.
   *
   * @return the default ClassLoader, or null if system ClassLoader is not accessible.
   */
  public static ClassLoader getDefaultClassLoader() {
    ClassLoader loader = getContextClassLoader();
    if (loader == null) {
      loader = ClassUtils.class.getClassLoader();
    }
    if (loader == null) {
      loader = ClassLoader.getSystemClassLoader();
    }
    return loader;
  }

  /**
   * Convert the '/' based resource path to '.' based class name.
   *
   * @param resource the resource path
   * @return the corresponding fully qualified class name
   */
  public static String resourceToClassName(String resource) {
    Objects.requireNonNull(resource, "resource name cannot be null.");
    return resource
        .replace(PATH_SEPARATOR, PACKAGE_SEPARATOR)
        .substring(0, resource.length() - CLASS_FILE_SUFFIX.length());
  }

  /**
   * Convert the class name to '/' based resource path.
   *
   * @param klass the fully qualified class name
   * @return the corresponding resource path
   */
  public static String classToResourceName(String klass) {
    Objects.requireNonNull(klass, "class name cannot be null.");
    return klass.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR) + CLASS_FILE_SUFFIX;
  }

  /**
   * Get the resource path of the given class.
   *
   * @param klass the class
   * @return the corresponding resource path
   */
  public static String classToResourceName(Class<?> klass) {
    Objects.requireNonNull(klass, "class cannot be null.");
    return classToResourceName(klass.getName());
  }

  /**
   * Check whether the given object is a CGLIB or ByteBuddy proxy.
   *
   * @param object the object to check
   * @return true if object is proxy
   */
  public static boolean isProxy(Object object) {
    return object != null && isProxyClass(object.getClass());
  }

  /**
   * Check whether the given class is a CGLIB or ByteBuddy proxy class, assuming no custom proxy
   * class naming is used.
   *
   * @param clazz the class to check
   * @return true of given class is a proxy class
   */
  public static boolean isProxyClass(Class<?> clazz) {
    return clazz != null && isProxyClassName(clazz.getName());
  }

  /**
   * Check whether the given class name is a CGLIB or ByteBuddy proxy class name, assuming no custom
   * class naming is used.
   *
   * @param className the class name to check
   * @return true if class name is proxy class name
   */
  public static boolean isProxyClassName(String className) {
    return className != null && className.contains(PROXY_CLASS_SEPARATOR);
  }

  /**
   * Get the real class of the given proxy class.
   *
   * @param proxyClass the proxy class whose first real super class is required.
   * @return the real super class of the given proxy class, or itself if it's not a proxy class
   */
  public static <T> Class<? super T> getRealClass(Class<T> proxyClass) {
    if (!isProxyClass(proxyClass)) {
      return proxyClass;
    }
    Class<? super T> zuper = proxyClass.getSuperclass();
    while (isProxyClass(zuper)) {
      zuper = getRealClass(zuper);
    }
    return zuper;
  }

  /**
   * Resolve the given resource location to a {@link URL}.
   *
   * @param location the resource location to resolve, either a pseudo 'classpath:' url, a 'file:'
   *     url or plain resource location.
   * @return a corresponding {@link URL} object
   * @throws FileNotFoundException if location can't be resolved.
   */
  public static URL getURL(String location) throws FileNotFoundException {
    Objects.requireNonNull(location, "Resource location must not be null");
    if (location.startsWith(CLASSPATH_URL_PREFIX)) {
      final String path = location.substring(CLASSPATH_URL_PREFIX.length());
      final URL url = getDefaultClassLoader().getResource(path);
      if (url == null) {
        throw new FileNotFoundException("Resource [" + path + "] not found in class path.");
      }
    }
    try {
      return new URL(location);
    } catch (MalformedURLException e) {
      try {
        return new File(location).toURI().toURL();
      } catch (MalformedURLException ex) {
        throw new FileNotFoundException("Resource [" + location + "] not found.");
      }
    }
  }

  /**
   * Finds the resource from the class path with the given location using default ClassLoader.
   *
   * @param location The resource location
   * @return an {@link URL} for reading the resource or null
   * @see ClassUtils#getDefaultClassLoader()
   * @see ClassLoader#getResource(String)
   */
  public static URL getResource(String location) {
    Objects.requireNonNull(location, "Resource location must not be null");
    String path = location;
    if (path.startsWith(CLASSPATH_URL_PREFIX)) {
      path = path.substring(CLASSPATH_URL_PREFIX.length());
    }
    return getDefaultClassLoader().getResource(path);
  }
}
