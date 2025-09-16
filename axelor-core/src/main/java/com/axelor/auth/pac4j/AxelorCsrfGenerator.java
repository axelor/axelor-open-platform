/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import jakarta.inject.Singleton;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.matching.matcher.csrf.DefaultCsrfTokenGenerator;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.util.Pac4jConstants;

/** CSRF token generator without rotating tokens for AJAX-heavy front-end */
@Singleton
public class AxelorCsrfGenerator extends DefaultCsrfTokenGenerator {

  @Override
  public String get(final WebContext context, final SessionStore sessionStore) {
    var token = (String) sessionStore.get(context, Pac4jConstants.CSRF_TOKEN).orElse(null);

    if (token == null) {
      token = CommonHelper.randomString(32);
      sessionStore.set(context, Pac4jConstants.CSRF_TOKEN, token);
    }

    return token;
  }
}
