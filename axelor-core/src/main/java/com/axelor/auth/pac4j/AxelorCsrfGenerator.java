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
