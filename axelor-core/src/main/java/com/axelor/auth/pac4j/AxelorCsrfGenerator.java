/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import jakarta.inject.Singleton;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.matching.matcher.csrf.DefaultCsrfTokenGenerator;
import org.pac4j.core.util.Pac4jConstants;

/**
 * CSRF token generator without rotating tokens
 *
 * <p>Default token rotation behavior is to generate a new token on every request, which is
 * incompatible with highly asynchronous front-end.
 */
@Singleton
public class AxelorCsrfGenerator extends DefaultCsrfTokenGenerator {

  public AxelorCsrfGenerator() {
    setRotateTokens(false);
  }

  @Override
  public String get(final WebContext context, final SessionStore sessionStore) {
    var token = super.get(context, sessionStore);

    // Default CsrfAuthorizer relies on expiration date, so refresh it unconditionally
    var expirationDate = System.currentTimeMillis() + getTtlInSeconds() * 1000L;
    sessionStore.set(context, Pac4jConstants.CSRF_TOKEN_EXPIRATION_DATE, expirationDate);

    return token;
  }
}
