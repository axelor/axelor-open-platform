/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
