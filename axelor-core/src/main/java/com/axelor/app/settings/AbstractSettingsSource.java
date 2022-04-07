package com.axelor.app.settings;

import java.util.Map;
import java.util.Set;

public abstract class AbstractSettingsSource {

  protected final Map<String, String> properties;

  public AbstractSettingsSource(Map<String, String> properties) {
    this.properties = properties;
  }

  public Map<String, String> getProperties() {
    return this.properties;
  }

  public Set<String> getPropertyNames() {
    return this.properties.keySet();
  }

  public String getValue(String propertyName) {
    return this.properties.get(propertyName);
  }
}
