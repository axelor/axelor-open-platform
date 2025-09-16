/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;

/** The class provides static helper methods to work with resources. */
public final class ResourceUtils {

  /**
   * Finds the resource from the class path with the given location using default ClassLoader.
   *
   * @param location The resource location
   * @return an {@link URL} for reading the resource or null
   * @see ClassUtils#getDefaultClassLoader()
   * @see ClassLoader#getResource(String)
   */
  public static URL getResource(String location) {
    return ClassUtils.getResource(location);
  }

  /**
   * Returns an input stream for reading the specified resource.
   *
   * @param location The resource location
   * @return An input stream for reading the resource or null
   * @see ResourceUtils#getResource(String)
   * @see ClassLoader#getResourceAsStream(String)
   */
  public static InputStream getResourceStream(String location) {
    final URL url = getResource(location);
    try {
      return url != null ? url.openStream() : null;
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Read {@link Properties} from the given file.
   *
   * @param file the properties file url
   * @return {@link Properties}
   * @throws IOException if unable to read file
   */
  public static Properties getProperties(URL file) throws IOException {
    Objects.requireNonNull(file, "file cannot be null");
    final Properties properties = new Properties();
    try (InputStream is = file.openStream()) {
      properties.load(is);
    }
    return properties;
  }

  /**
   * Read {@link Properties} from the given file.
   *
   * @param file the properties file url
   * @return {@link Properties}
   * @throws IOException if unable to read file
   */
  public static Properties getProperties(File file) throws IOException {
    Objects.requireNonNull(file, "file cannot be null");
    final Properties properties = new Properties();
    try (InputStream is = new FileInputStream(file)) {
      properties.load(is);
    }
    return properties;
  }
}
