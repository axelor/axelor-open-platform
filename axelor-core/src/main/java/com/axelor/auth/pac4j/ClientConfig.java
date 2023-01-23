/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j;

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
}
