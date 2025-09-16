/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.config;

import com.axelor.auth.pac4j.AxelorCallbackLogic;
import com.axelor.auth.pac4j.AxelorCsrfAuthorizer;
import com.axelor.auth.pac4j.AxelorCsrfMatcher;
import com.axelor.auth.pac4j.AxelorLogoutLogic;
import com.axelor.auth.pac4j.AxelorProfileManager;
import com.axelor.auth.pac4j.AxelorSecurityLogic;
import com.axelor.auth.pac4j.AxelorUserAuthorizer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.jee.context.JEEContextFactory;
import org.pac4j.jee.context.session.JEESessionStoreFactory;

@Singleton
public class BaseConfig extends Config {

  @Inject
  public BaseConfig(
      Clients clients,
      AxelorUserAuthorizer userAuthorizer,
      AxelorCsrfAuthorizer csrfAuthorizer,
      AxelorCsrfMatcher csrfMatcher,
      AxelorSecurityLogic securityLogic,
      AxelorCallbackLogic callbackLogic,
      AxelorLogoutLogic logoutLogic) {

    super(clients);

    addAuthorizer(AxelorUserAuthorizer.USER_AUTHORIZER_NAME, userAuthorizer);
    addAuthorizer(AxelorCsrfAuthorizer.CSRF_AUTHORIZER_NAME, csrfAuthorizer);
    addMatcher(AxelorCsrfMatcher.CSRF_MATCHER_NAME, csrfMatcher);

    setSecurityLogic(securityLogic);
    setCallbackLogic(callbackLogic);
    setLogoutLogic(logoutLogic);

    setWebContextFactoryIfUndefined(JEEContextFactory.INSTANCE);
    setSessionStoreFactoryIfUndefined(JEESessionStoreFactory.INSTANCE);
    setProfileManagerFactoryIfUndefined(AxelorProfileManager::new);
  }
}
