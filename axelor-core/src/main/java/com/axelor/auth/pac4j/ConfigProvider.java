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
package com.axelor.auth.pac4j;

import com.google.inject.Provider;
import java.util.Collections;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;

@Singleton
public class ConfigProvider implements Provider<Config> {

  private final Config config;

  @Inject
  public ConfigProvider(
      Clients clients, AxelorCsrfAuthorizer csrfAuthorizer, AxelorCsrfMatcher csrfMatcher) {
    final Map<String, Authorizer> authorizers =
        Collections.singletonMap(AxelorCsrfAuthorizer.CSRF_AUTHORIZER_NAME, csrfAuthorizer);
    config = new Config(clients, authorizers);
    config.addMatcher(AxelorCsrfMatcher.CSRF_MATCHER_NAME, csrfMatcher);
  }

  @Override
  public Config get() {
    return config;
  }
}
