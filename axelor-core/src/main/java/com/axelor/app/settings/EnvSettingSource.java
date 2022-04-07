package com.axelor.app.settings;

import static java.util.stream.Collectors.toMap;

import java.util.Map;

public class EnvSettingSource extends MapSettingsSource {

  public EnvSettingSource() {
    super(getEnvProperties());
  }

  static Map<String, String> getEnvProperties() {
    return parse(System.getenv());
  }

  static Map<String, String> parse(Map<String, String> env) {
    return env.entrySet().stream()
        .filter(e -> e.getKey().startsWith(SettingsUtils.ENV_PREFIX))
        .collect(toMap(EnvSettingSource::processKey, Map.Entry::getValue));
  }

  static String processKey(Map.Entry<String, String> e) {
    return e.getKey()
        .replaceFirst(SettingsUtils.ENV_PREFIX, "")
        .replace('_', '.')
        .replace(' ', '\0')
        .toLowerCase();
  }
}
