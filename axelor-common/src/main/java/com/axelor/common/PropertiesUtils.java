/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/** This class defines from static helper methods to deal with {@link Properties}. */
public class PropertiesUtils {

  public PropertiesUtils() {}

  public static Properties loadProperties(File file) throws IOException {
    return loadProperties(file.toPath());
  }

  public static Properties loadProperties(Path path) throws IOException {
    return loadProperties(path.toUri().toURL());
  }

  public static Properties loadProperties(URL resource) throws IOException {
    Properties props = new Properties();
    if (resource == null) {
      return props;
    }
    try (InputStream stream = resource.openStream()) {
      props.load(stream);
    }
    return props;
  }

  public static Map<String, String> propertiesToMap(Properties properties) {
    Map<String, String> map = new HashMap<>();
    synchronized (properties) {
      for (Map.Entry<Object, Object> entry : properties.entrySet()) {
        map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
      }

      return map;
    }
  }
}
