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
package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AuthPac4jInfo;
import java.util.Optional;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.UnauthorizedAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.http.ajax.DefaultAjaxRequestResolver;
import org.pac4j.core.redirect.RedirectionActionBuilder;

public class AxelorAjaxRequestResolver extends DefaultAjaxRequestResolver {

  @Override
  public boolean isAjax(WebContext context) {
    return super.isAjax(context) || AuthPac4jInfo.isXHR(context);
  }

  @Override
  public HttpAction buildAjaxResponse(
      WebContext context, RedirectionActionBuilder redirectionActionBuilder) {
    if (isAjax(context)
        && getUrl(context, redirectionActionBuilder)
            .filter(url -> "login.jsp".equals(url))
            .isPresent()) {
      return UnauthorizedAction.INSTANCE;
    }
    return super.buildAjaxResponse(context, redirectionActionBuilder);
  }

  protected Optional<String> getUrl(
      WebContext context, RedirectionActionBuilder redirectionActionBuilder) {
    return redirectionActionBuilder
        .getRedirectionAction(context)
        .filter(action -> action instanceof WithLocationAction)
        .map(action -> ((WithLocationAction) action).getLocation());
  }
}
