/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AuthPac4jInfo;
import java.util.Optional;
import javax.inject.Singleton;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.UnauthorizedAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.http.ajax.DefaultAjaxRequestResolver;
import org.pac4j.core.redirect.RedirectionActionBuilder;

@Singleton
public class AxelorAjaxRequestResolver extends DefaultAjaxRequestResolver {

  @Override
  public boolean isAjax(WebContext context, SessionStore sessionStore) {
    return super.isAjax(context, sessionStore) || AuthPac4jInfo.isXHR(context);
  }

  @Override
  public HttpAction buildAjaxResponse(
      WebContext context,
      SessionStore sessionStore,
      RedirectionActionBuilder redirectionActionBuilder) {
    if (isAjax(context, sessionStore)
        && getUrl(context, sessionStore, redirectionActionBuilder)
            .filter(url -> url.endsWith(AxelorFormClient.LOGIN_URL))
            .isPresent()) {
      return UnauthorizedAction.INSTANCE;
    }
    return super.buildAjaxResponse(context, sessionStore, redirectionActionBuilder);
  }

  protected Optional<String> getUrl(
      WebContext context,
      SessionStore sessionStore,
      RedirectionActionBuilder redirectionActionBuilder) {
    return redirectionActionBuilder
        .getRedirectionAction(context, sessionStore)
        .filter(WithLocationAction.class::isInstance)
        .map(action -> ((WithLocationAction) action).getLocation());
  }
}
