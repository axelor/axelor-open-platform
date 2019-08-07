/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.common.StringUtils;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import javax.servlet.ServletContext;
import org.pac4j.core.client.Client;

public abstract class AuthPac4jModuleMultiClient extends AuthPac4jModuleForm {
  public AuthPac4jModuleMultiClient(ServletContext servletContext) {
    super(servletContext);
  }

  protected void addFormClientIfNotExclusive(Map<String, Map<String, String>> allSettings) {
    if (allSettings.size() == 1
        && allSettings
            .values()
            .iterator()
            .next()
            .getOrDefault("exclusive", "false")
            .equals("true")) {
      return;
    }
    addFormClient();
  }

  protected void addCentralClients(
      Map<String, Map<String, String>> allSettings,
      Map<String, Function<Map<String, String>, Client<?, ?>>> providers) {
    for (final Entry<String, Map<String, String>> entry : allSettings.entrySet()) {
      final String provider = entry.getKey();
      final Map<String, String> settings = entry.getValue();
      final Function<Map<String, String>, Client<?, ?>> setup = providers.get(provider);
      final Client<?, ?> client =
          setup != null ? setup.apply(settings) : setupGeneric(settings, provider);
      addClient(client);
    }
  }

  protected abstract Client<?, ?> setupGeneric(Map<String, String> settings, String providerName);

  protected static Map<String, Map<String, String>> getAuthSettings(String prefix) {
    final Map<String, Map<String, String>> all = new LinkedHashMap<>();
    final AppSettings settings = AppSettings.get();

    for (final String key : settings.getProperties().stringPropertyNames()) {
      if (key.startsWith(prefix)) {
        final String[] parts = key.substring(prefix.length()).split("\\.", 2);
        if (parts.length > 1) {
          final String provider = parts[0];
          final String config = parts[1];
          final String value = settings.get(key);
          if (StringUtils.notBlank(value)) {
            final Map<String, String> map = all.computeIfAbsent(provider, k -> new HashMap<>());
            map.put(config, value);
          }
        }
      }
    }

    return all;
  }
}
