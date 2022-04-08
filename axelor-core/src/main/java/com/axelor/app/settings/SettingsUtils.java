package com.axelor.app.settings;

import java.util.Map;
import java.util.stream.Collectors;

public class SettingsUtils {

  public static final String CONFIG_FILE_NAME = "axelor-config";
  public static final String EXTERNAL_CONFIG_SYSTEM_PROP = "axelor.config";
  public static final String EXTERNAL_CONFIG_ENV = "AXELOR_CONFIG";

  public static final String ENV_CONFIG_PREFIX = "AXELOR_CONFIG_";
  public static final String SYSTEM_CONFIG_PREFIX = "axelor.config.";

  public static final String CONFIG_ENCRYPTOR_PREFIX = "config.encryptor";

  public SettingsUtils() {}

  public static Map<String, String> extractProperties(Map<String, String> source, String prefix) {
    return source.entrySet().stream()
        .filter(e -> e.getKey().startsWith(prefix))
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public static final String ENCRYPT_PREFIX = "ENC(";
  public static final String ENCRYPT_SUFFIX = ")";

  public static boolean isEncrypted(String property) {
    if (property == null) {
      return false;
    }
    final String trimmedValue = property.trim();
    return (trimmedValue.startsWith(ENCRYPT_PREFIX) && trimmedValue.endsWith(ENCRYPT_SUFFIX));
  }

  public static String unwrapEncryptedValue(String property) {
    return property.substring(
        ENCRYPT_PREFIX.length(), (property.length() - ENCRYPT_SUFFIX.length()));
  }
}
