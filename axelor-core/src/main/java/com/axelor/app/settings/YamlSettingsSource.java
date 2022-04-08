package com.axelor.app.settings;

import com.axelor.common.YamlUtils;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YamlSettingsSource extends AbstractSettingsSource {

  private static final Logger LOG = LoggerFactory.getLogger(YamlSettingsSource.class);

  public YamlSettingsSource(URL url) {
    this(loadYaml(url));
  }

  public YamlSettingsSource(Map<String, Object> values) {
    super(YamlUtils.getFlattenedMap(values));
  }

  private static Map<String, Object> loadYaml(URL resource) {

    try {
      return YamlUtils.loadYaml(resource);
    } catch (Exception e) {
      LOG.trace("Unable to open {} yaml file", resource);
    }

    return new HashMap<>();
  }
}
