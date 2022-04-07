package com.axelor.app.settings;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class SettingsUtils {

  public static final String CONFIG_FILE_NAME = "application";
  public static final String EXTERNAL_CONFIG_SYSTEM_PROP = "axelor.config";
  public static final String EXTERNAL_CONFIG_ENV = "AXELOR_CONFIG";

  public static final String ENV_PREFIX = "AXELOR_CONFIG_";
  public static final String SYSTEM_PREFIX = "axelor.config.";

  public SettingsUtils() {}

  public static Map<String, String> propertiesToMap(Properties properties) {
    Map<String, String> map = new HashMap<>();
    synchronized (properties) {
      Iterator iterator = properties.entrySet().iterator();

      while (iterator.hasNext()) {
        Map.Entry<Object, Object> entry = (Map.Entry) iterator.next();
        map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
      }

      return map;
    }
  }
}
