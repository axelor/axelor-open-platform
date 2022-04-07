package com.axelor.app.settings;

import static java.util.stream.Collectors.toMap;

import java.util.Map;

public class SystemSettingSource extends MapSettingsSource {

  public SystemSettingSource() {
    super(getEnvProperties());
  }

  static Map<String, String> getEnvProperties() {
    return parse(SettingsUtils.propertiesToMap(System.getProperties()));
  }

  static Map<String, String> parse(Map<String, String> env) {
    return env.entrySet().stream()
        .filter(e -> e.getKey().startsWith(SettingsUtils.SYSTEM_PREFIX))
        .collect(toMap(SystemSettingSource::processKey, Map.Entry::getValue));
  }

  static String processKey(Map.Entry<String, String> e) {
    return e.getKey()
        .replaceFirst(SettingsUtils.SYSTEM_PREFIX, "")
        .replace(' ', '\0')
        .toLowerCase();
  }
}
