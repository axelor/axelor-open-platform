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

import java.util.Optional;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.matching.matcher.csrf.DefaultCsrfTokenGenerator;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.util.Pac4jConstants;

/**
 * Default CSRF token generator since pac4j 5 generates tokens per HTTP request.
 *
 * <p>This reverts to pac4j 4 CSRF token generator behavior, because of asynchronous request issues
 * with current web client.
 *
 * <p>Code taken from:
 * https://github.com/pac4j/pac4j/blob/4.5.x/pac4j-core/src/main/java/org/pac4j/core/matching/matcher/csrf/DefaultCsrfTokenGenerator.java
 */
public class AxelorCsrfGenerator extends DefaultCsrfTokenGenerator {

  @Override
  public String get(WebContext context, SessionStore sessionStore) {
    Optional<String> token = getTokenFromSession(context, sessionStore);
    if (!token.isPresent()) {
      synchronized (this) {
        token = getTokenFromSession(context, sessionStore);
        if (!token.isPresent()) {
          token = Optional.of(CommonHelper.randomString(32));
          sessionStore.set(context, Pac4jConstants.CSRF_TOKEN, token.get());
        }
      }
    }
    return token.get();
  }

  @SuppressWarnings("unchecked")
  protected Optional<String> getTokenFromSession(WebContext context, SessionStore sessionStore) {
    return (Optional<String>) (Optional<?>) sessionStore.get(context, Pac4jConstants.CSRF_TOKEN);
  }
}
