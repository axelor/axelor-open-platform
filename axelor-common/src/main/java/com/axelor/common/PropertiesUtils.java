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
