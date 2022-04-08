package com.axelor.app.settings;

import com.axelor.common.PropertiesUtils;
import java.net.URL;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesSettingsSource extends MapSettingsSource {

  private static final Logger LOG = LoggerFactory.getLogger(PropertiesSettingsSource.class);

  public PropertiesSettingsSource(URL url) {
    this(loadProperties(url));
  }

  public PropertiesSettingsSource(Properties properties) {
    super(PropertiesUtils.propertiesToMap(properties));
  }

  private static Properties loadProperties(URL resource) {
    try {
      return PropertiesUtils.loadProperties(resource);
    } catch (Exception e) {
      LOG.trace("Unable to open {} properties file.", resource);
    }
    return new Properties();
  }
}
