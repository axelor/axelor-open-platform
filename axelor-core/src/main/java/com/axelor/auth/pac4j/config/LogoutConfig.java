/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j.config;

import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.auth.pac4j.AxelorCallbackLogic;
import com.axelor.auth.pac4j.AxelorCsrfAuthorizer;
import com.axelor.auth.pac4j.AxelorCsrfMatcher;
import com.axelor.auth.pac4j.AxelorLogoutLogic;
import com.axelor.auth.pac4j.AxelorSecurityLogic;
import com.axelor.auth.pac4j.AxelorUserAuthorizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Objects;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.exception.http.NoContentAction;
import org.pac4j.core.exception.http.OkAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.jee.http.adapter.JEEHttpActionAdapter;

@Singleton
public class LogoutConfig extends BaseConfig {

  @Inject
  public LogoutConfig(
      Clients clients,
      AxelorUserAuthorizer userAuthorizer,
      AxelorCsrfAuthorizer csrfAuthorizer,
      AxelorCsrfMatcher csrfMatcher,
      AxelorSecurityLogic securityLogic,
      AxelorCallbackLogic callbackLogic,
      AxelorLogoutLogic logoutLogic,
      AuthPac4jInfo info) {

    super(
        clients,
        userAuthorizer,
        csrfAuthorizer,
        csrfMatcher,
        securityLogic,
        callbackLogic,
        logoutLogic);

    setHttpActionAdapter(
        (action, context) -> {
          if (action instanceof WithLocationAction withLocationAction
              && AuthPac4jInfo.isXHR(context)) {
            final String url = withLocationAction.getLocation();
            if (Objects.equals(url, info.getBaseUrl())) {
              action = NoContentAction.INSTANCE;
            } else {
              context.setResponseHeader(
                  HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.APPLICATION_JSON);
              final ObjectMapper mapper = new ObjectMapper();
              final ObjectNode json = mapper.createObjectNode();
              json.put("redirectUrl", url);
              action = new OkAction(json.toString());
            }
          }
          return JEEHttpActionAdapter.INSTANCE.adapt(action, context);
        });
  }
}
