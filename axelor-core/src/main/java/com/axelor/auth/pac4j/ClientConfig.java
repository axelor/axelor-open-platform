/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClientConfig {
  private String client;
  private String configuration;
  private String title;
  private String icon;
  private boolean exclusive;

  public String getClient() {
    return client;
  }

  public void setClient(String client) {
    this.client = client;
  }

  public String getConfiguration() {
    return configuration;
  }

  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public boolean isExclusive() {
    return exclusive;
  }

  public void setExclusive(boolean exclusive) {
    this.exclusive = exclusive;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ClientConfig clientConfig = new ClientConfig();

    public ClientConfig build() {
      return clientConfig;
    }

    public Builder client(String client) {
      clientConfig.setClient(client);
      return this;
    }

    public Builder configuration(String configuration) {
      clientConfig.setConfiguration(configuration);
      return this;
    }

    public Builder title(String title) {
      clientConfig.setTitle(title);
      return this;
    }

    public Builder icon(String icon) {
      clientConfig.setIcon(icon);
      return this;
    }

    public Builder exclusive() {
      clientConfig.setExclusive(true);
      return this;
    }
  }

  public Map<String, Object> toMap() {
    final Map<String, Object> map = new HashMap<>();

    map.put("client", client);

    if (configuration != null) {
      map.put("configuration", configuration);
    }
    if (title != null) {
      map.put("title", title);
    }
    if (icon != null) {
      map.put("icon", icon);
    }
    if (exclusive) {
      map.put("exclusive", exclusive);
    }

    return Collections.unmodifiableMap(map);
  }
}
