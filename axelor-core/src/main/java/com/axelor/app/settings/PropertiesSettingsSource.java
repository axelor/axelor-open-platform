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
