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
