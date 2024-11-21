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
package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AuthPac4jInfo;
import jakarta.inject.Singleton;
import java.util.Optional;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.UnauthorizedAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.http.ajax.DefaultAjaxRequestResolver;
import org.pac4j.core.redirect.RedirectionActionBuilder;

@Singleton
public class AxelorAjaxRequestResolver extends DefaultAjaxRequestResolver {

  @Override
  public boolean isAjax(CallContext ctx) {
    return super.isAjax(ctx) || AuthPac4jInfo.isXHR(ctx);
  }

  @Override
  public HttpAction buildAjaxResponse(
      CallContext ctx, RedirectionActionBuilder redirectionActionBuilder) {
    if (isAjax(ctx)
        && getUrl(ctx, redirectionActionBuilder)
            .filter(url -> url.endsWith(AxelorFormClient.LOGIN_URL))
            .isPresent()) {
      return new UnauthorizedAction();
    }
    return super.buildAjaxResponse(ctx, redirectionActionBuilder);
  }

  protected Optional<String> getUrl(
      CallContext ctx, RedirectionActionBuilder redirectionActionBuilder) {
    return redirectionActionBuilder
        .getRedirectionAction(ctx)
        .filter(WithLocationAction.class::isInstance)
        .map(action -> ((WithLocationAction) action).getLocation());
  }
}
