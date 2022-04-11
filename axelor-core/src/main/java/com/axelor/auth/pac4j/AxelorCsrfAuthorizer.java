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

import static org.pac4j.core.context.WebContextHelper.isDelete;
import static org.pac4j.core.context.WebContextHelper.isPatch;
import static org.pac4j.core.context.WebContextHelper.isPost;
import static org.pac4j.core.context.WebContextHelper.isPut;

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.pac4j.core.authorization.authorizer.CsrfAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.Pac4jConstants;

@Singleton
public class AxelorCsrfAuthorizer extends CsrfAuthorizer {
  public static final String CSRF_AUTHORIZER_NAME = "axelorCsrf";

  @Inject
  public AxelorCsrfAuthorizer() {
    this(AuthPac4jModule.CSRF_HEADER_NAME, AuthPac4jModule.CSRF_HEADER_NAME);
  }

  public AxelorCsrfAuthorizer(String parameterName, final String headerName) {
    super(parameterName, headerName);
  }

  @Override
  public boolean isAuthorized(
      WebContext context, SessionStore sessionStore, List<UserProfile> profiles) {
    // Don't need CSRF check for native clients
    if (AuthPac4jInfo.isNativeClient(context)) {
      return true;
    }

    return internalIsAuthorized(context, sessionStore, profiles);
  }

  /**
   * Matches {@link AxelorCsrfGenerator} behavior.
   *
   * <p>Code taken from:
   * https://github.com/pac4j/pac4j/blob/4.5.x/pac4j-core/src/main/java/org/pac4j/core/authorization/authorizer/CsrfAuthorizer.java#L40
   *
   * @param context the web context
   * @param sessionStore the session store
   * @param profiles the user profiles
   * @return whether the access is authorized
   */
  protected boolean internalIsAuthorized(
      WebContext context, SessionStore sessionStore, List<UserProfile> profiles) {
    final boolean checkRequest =
        isCheckAllRequests()
            || isPost(context)
            || isPut(context)
            || isPatch(context)
            || isDelete(context);
    if (checkRequest) {
      final String parameterToken = context.getRequestParameter(getParameterName()).orElse(null);
      final String headerToken = context.getRequestHeader(getHeaderName()).orElse(null);
      @SuppressWarnings("unchecked")
      final Optional<String> sessionToken =
          (Optional<String>) (Optional<?>) sessionStore.get(context, Pac4jConstants.CSRF_TOKEN);
      return sessionToken.isPresent()
          && (sessionToken.get().equals(parameterToken) || sessionToken.get().equals(headerToken));
    } else {
      return true;
    }
  }
}
