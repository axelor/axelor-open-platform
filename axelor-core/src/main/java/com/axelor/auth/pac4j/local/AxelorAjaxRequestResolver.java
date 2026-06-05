/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.auth.pac4j.AxelorUrlResolver;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.UnauthorizedAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.http.ajax.DefaultAjaxRequestResolver;
import org.pac4j.core.http.url.UrlResolver;
import org.pac4j.core.redirect.RedirectionActionBuilder;

@Singleton
public class AxelorAjaxRequestResolver extends DefaultAjaxRequestResolver {

  private final UrlResolver urlResolver;

  @Inject
  public AxelorAjaxRequestResolver(AxelorUrlResolver urlResolver) {
    this.urlResolver = urlResolver;
  }

  @Override
  public boolean isAjax(CallContext ctx) {
    return super.isAjax(ctx) || AuthPac4jInfo.isXHR(ctx);
  }

  @Override
  public HttpAction buildAjaxResponse(
      CallContext ctx, RedirectionActionBuilder redirectionActionBuilder) {
    if (isAjax(ctx)
        && getUrl(ctx, redirectionActionBuilder)
            .filter(url -> url.equals(getFormLoginUrl(ctx.webContext())))
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

  protected String getFormLoginUrl(WebContext context) {
    return urlResolver.compute(AxelorFormClient.LOGIN_URL, context);
  }
}
