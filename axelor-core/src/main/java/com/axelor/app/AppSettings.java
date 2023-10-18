/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.app;

import com.axelor.app.internal.AppFilter;
import com.axelor.app.settings.SettingsBuilder;
import com.axelor.common.StringUtils;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AppSettings {

  private Map<String, String> properties;

  private static AppSettings instance;

  private AppSettings() {
    properties = new SettingsBuilder().buildSettings();
  }

  public static AppSettings get() {
    if (instance == null) {
      instance = new AppSettings();
    }
    return instance;
  }

  public String get(String key) {
    return sub(properties.get(key));
  }

  public String get(String key, String defaultValue) {
    String value = properties.getOrDefault(key, defaultValue);
    if (StringUtils.isBlank(value)) {
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
      return Integer.parseInt(get(key));
    } catch (Exception e) {
      // ignore
    }
    return defaultValue;
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    final String value = get(key);
    return StringUtils.notBlank(value) ? Boolean.parseBoolean(value) : defaultValue;
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
    final LocalDate now = LocalDate.now();
    return value
        .replace("{year}", "" + now.getYear())
        .replace("{month}", "" + now.getMonthValue())
        .replace("{day}", "" + now.getDayOfMonth())
        .replace("{java.io.tmpdir}", System.getProperty("java.io.tmpdir"))
        .replace("{user.home}", System.getProperty("user.home"))
        .replace("{user.dir}", System.getProperty("user.dir"));
  }

  /**
   * Get the application base URL.
   *
   * <p>This method tries to calculate the base url from current http request. If the method is
   * called outside of http request scope, it returns the value of <code>application.base-url</code>
   * configuration setting.
   *
   * @return application base url
   */
  public String getBaseURL() {
    String url = AppFilter.getBaseURL();
    if (url == null) {
      url = get(AvailableAppSettings.APPLICATION_BASE_URL);
    }
    if (StringUtils.notBlank(url) && url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    return url;
  }

  public boolean isProduction() {
    return !"dev".equals(get(AvailableAppSettings.APPLICATION_MODE, "dev"));
  }

  /**
   * For internal use only.
   *
   * @return the internal properties store
   */
  public Map<String, String> getInternalProperties() {
    return properties;
  }

  /**
   * For internal use only.
   *
   * @return the internal properties store
   */
  public Map<String, String> getProperties() {
    return Collections.unmodifiableMap(properties);
  }

  /**
   * Get all properties keys
   *
   * @return an unmodifiable {@link Set} of keys
   */
  public Set<String> getPropertiesKeys() {
    return properties.keySet().stream().collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Get properties keys starting with the given prefix
   *
   * @return an unmodifiable {@link Set} of keys
   */
  public Set<String> getPropertiesKeysStartingWith(String prefix) {
    return properties.keySet().stream()
        .filter(k -> k.startsWith(prefix))
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Get properties where the key start with the given prefix
   *
   * @return an unmodifiable {@link Map} of matching properties
   */
  public Map<String, String> getPropertiesStartingWith(String prefix) {
    return properties.entrySet().stream()
        .filter(e -> e.getKey().startsWith(prefix))
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Enable specified feature
   *
   * @param name name of the feature
   */
  public void enableFeature(String name) {
    properties.put(AvailableAppSettings.FEATURE_PREFIX + name, "true");
  }

  /**
   * Disable specified feature
   *
   * @param name name of the feature
   */
  public void disableFeature(String name) {
    properties.put(AvailableAppSettings.FEATURE_PREFIX + name, "false");
  }

  /**
   * Check whether specified feature is enabled
   *
   * @param name name of the feature
   * @return true if feature is enabled false otherwise
   */
  public boolean hasFeature(String name) {
    return properties
        .getOrDefault(AvailableAppSettings.FEATURE_PREFIX + name, "false")
        .equals("true");
  }
}
