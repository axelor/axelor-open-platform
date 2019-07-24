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
package com.axelor.app;

import com.axelor.app.internal.AppFilter;
import com.axelor.common.ResourceUtils;
import com.axelor.common.StringUtils;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AppSettings {

  private static final String DEFAULT_CONFIG_LOCATION = "application.properties";
  private static final String CUSTOM_CONFIG_LOCATION = "axelor.config";

  private Properties properties;

  private static AppSettings instance;

  private AppSettings() {
    String config = System.getProperty(CUSTOM_CONFIG_LOCATION);
    InputStream stream = null;
    try {
      if (StringUtils.isBlank(config)) {
        stream = ResourceUtils.getResourceStream(config = DEFAULT_CONFIG_LOCATION);
      } else {
        stream = new FileInputStream(config);
      }
      try {
        properties = new LinkedProperties();
        properties.load(stream);
      } finally {
        stream.close();
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to load application settings: " + config);
    }
  }

  public static AppSettings get() {
    if (instance == null) {
      instance = new AppSettings();
    }
    return instance;
  }

  public String get(String key) {
    return sub(properties.getProperty(key));
  }

  public String get(String key, String defaultValue) {
    String value = properties.getProperty(key, defaultValue);
    if (value == null || "".equals(value.trim())) {
      value = defaultValue;
    }
    return sub(value);
  }

  public List<String> getList(String key) {
    return Arrays.stream(get(key, "").trim().split("\\s*,\\s*"))
        .filter(StringUtils::notBlank)
        .collect(Collectors.toList());
  }

  public <T> List<T> getList(String key, Function<String, T> mapper) {
    return getList(key).stream().map(mapper).collect(Collectors.toList());
  }

  public int getInt(String key, int defaultValue) {
    try {
      return Integer.parseInt(get(key).toString());
    } catch (Exception e) {
    }
    return defaultValue;
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    try {
      return Boolean.parseBoolean(get(key).toString());
    } catch (Exception e) {
    }
    return defaultValue;
  }

  public String getPath(String key, String defaultValue) {
    String path = get(key, defaultValue);
    if (path == null) {
      return null;
    }
    return sub(path);
  }

  private String sub(String value) {
    if (value == null) {
      return null;
    }
    final Calendar cal = Calendar.getInstance();
    return value
        .replace("{year}", "" + cal.get(Calendar.YEAR))
        .replace("{month}", "" + cal.get(Calendar.MONTH))
        .replace("{day}", "" + cal.get(Calendar.DAY_OF_MONTH))
        .replace("{java.io.tmpdir}", System.getProperty("java.io.tmpdir"))
        .replace("{user.home}", System.getProperty("user.home"));
  }

  /**
   * Get the application base URL.
   *
   * <p>This method tries to calculate the base url from current http request. If the method is
   * called outside of http request scope, it returns the value of <code>application.baseUrl</code>
   * configuration setting.
   *
   * @return application base url
   */
  public String getBaseURL() {
    String url = AppFilter.getBaseURL();
    if (url == null) {
      url = get("application.baseUrl");
    }
    return url;
  }

  public boolean isProduction() {
    return !"dev".equals(get("application.mode", "dev"));
  }

  /**
   * For internal use only.
   *
   * @return the internal properties store
   */
  public Properties getProperties() {
    return properties;
  }

  /** Properties with keys in order of insertion */
  public static class LinkedProperties extends Properties {

    private static final long serialVersionUID = -1869328576799427860L;
    private final Set<Object> keys = new LinkedHashSet<>();

    @Override
    public synchronized Object put(Object key, Object value) {
      keys.add(key);
      return super.put(key, value);
    }

    @Override
    public synchronized void putAll(Map<? extends Object, ? extends Object> t) {
      keys.addAll(t.keySet());
      super.putAll(t);
    }

    @Override
    public synchronized Object remove(Object key) {
      keys.remove(key);
      return super.remove(key);
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
      boolean removed = super.remove(key, value);

      if (removed) {
        keys.remove(key);
      }

      return removed;
    }

    @Override
    public Set<Object> keySet() {
      return keys;
    }

    @Override
    public synchronized boolean equals(Object o) {
      return super.equals(o);
    }

    @Override
    public synchronized int hashCode() {
      return super.hashCode();
    }
  }
}
