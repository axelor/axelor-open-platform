/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.app.settings;

import com.axelor.common.ClassUtils;
import com.axelor.common.StringUtils;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jasypt.encryption.StringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(SettingsBuilder.class);

  private StringEncryptor encryptor;

  public SettingsBuilder() {}

  public Map<String, String> buildSettings() {
    Map<String, String> values = Collections.synchronizedMap(new LinkedHashMap<>());
    for (AbstractSettingsSource source : getSettingsSources()) {
      values.putAll(source.getProperties());
    }
    return parseProps(values);
  }

  private Map<String, String> parseProps(Map<String, String> props) {
    for (Map.Entry<String, String> entry : props.entrySet()) {
      if (SettingsUtils.isEncrypted(entry.getValue())) {
        if (encryptor == null) {
          // Init encryptor only if encoded values are present
          // This is time-consuming
          initEncryptor(props);
        }
        decode(entry);
      }
    }
    return props;
  }

  private void decode(Map.Entry<String, String> entry) {
    if (!SettingsUtils.isEncrypted(entry.getValue())) {
      return;
    }

    try {
      entry.setValue(
          encryptor.decrypt(SettingsUtils.unwrapEncryptedValue(entry.getValue().trim())));
    } catch (Exception e) {
      LOG.error("a error : ", e);
      throw new RuntimeException("Unable to decrypt property: " + entry.getKey(), e);
    }
  }

  private void initEncryptor(Map<String, String> props) {
    encryptor =
        new StringEncryptorBuilder(
                SettingsUtils.extractProperties(props, SettingsUtils.CONFIG_ENCRYPTOR_PREFIX))
            .build();
  }

  private List<AbstractSettingsSource> getSettingsSources() {
    List<AbstractSettingsSource> sources = new ArrayList<>();
    addDefaultSource(sources);
    addExternalSource(sources);
    sources.add(new EnvSettingSource());
    sources.add(new SystemSettingSource());
    return sources;
  }

  private void addExternalSource(List<AbstractSettingsSource> sources) {
    // external config from system properties takes preference
    // over external config defined in env
    String config = System.getProperty(SettingsUtils.EXTERNAL_CONFIG_SYSTEM_PROP);
    if (StringUtils.isBlank(config)) {
      config = System.getenv(SettingsUtils.EXTERNAL_CONFIG_ENV);
    }
    if (StringUtils.isBlank(config)) {
      return;
    }

    try {
      URL resource = new File(config).toURI().toURL();
      if (getYamlFileExtensions().contains(config.substring(config.lastIndexOf(".") + 1))) {
        sources.add(new YamlSettingsSource(resource));
      } else {
        sources.add(new PropertiesSettingsSource(resource));
      }
    } catch (Exception e) {
      LOG.debug("Unable to load {} config file : {}", config, e);
    }
  }

  private void addDefaultSource(List<AbstractSettingsSource> sources) {
    URL propResource = getDefaultPropertiesFile();
    if (propResource != null) {
      sources.add(new PropertiesSettingsSource(propResource));
    }

    URL yamlResource = getDefaultYamlFile();
    if (yamlResource != null) {
      sources.add(new YamlSettingsSource(yamlResource));
    }
  }

  private URL getDefaultPropertiesFile() {
    String fileName = String.format("%s.properties", SettingsUtils.CONFIG_FILE_NAME);
    return ClassUtils.getResource(fileName);
  }

  private URL getDefaultYamlFile() {
    for (String ext : getYamlFileExtensions()) {
      String fileName = String.format("%s.%s", SettingsUtils.CONFIG_FILE_NAME, ext);
      URL resource = ClassUtils.getResource(fileName);
      if (resource != null) {
        return resource;
      }
    }
    return null;
  }

  private List<String> getYamlFileExtensions() {
    return List.of("yaml", "yml");
  }
}
