/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
