package com.axelor.app.settings;

import java.io.InputStream;
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
    super(SettingsUtils.propertiesToMap(properties));
  }

  private static Properties loadProperties(URL resource) {
    Properties props = new Properties();
    if (resource == null) {
      return props;
    }
    try (InputStream stream = resource.openStream()) {
      props.load(stream);
    } catch (Exception e) {
      LOG.trace("Unable to open {} properties file.", resource);
    }
    return props;
  }
}
