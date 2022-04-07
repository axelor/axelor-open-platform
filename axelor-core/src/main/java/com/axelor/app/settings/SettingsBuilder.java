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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(SettingsBuilder.class);

  public SettingsBuilder() {}

  public Map<String, String> buildSettings() {
    Map<String, String> values = Collections.synchronizedMap(new LinkedHashMap<>());
    for (AbstractSettingsSource source : getSettingsSources()) {
      values.putAll(source.getProperties());
    }
    return values;
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
