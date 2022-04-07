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

import com.axelor.common.StringUtils;
import io.buji.pac4j.profile.ShiroProfileManager;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import javax.inject.Inject;
import org.pac4j.core.client.finder.DefaultCallbackClientFinder;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.OkAction;
import org.pac4j.core.exception.http.RedirectionActionHelper;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.util.Pac4jConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AxelorCallbackLogic extends DefaultCallbackLogic<Object, JEEContext> {

  private final AxelorCsrfMatcher csrfMatcher;

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject
  public AxelorCallbackLogic(
      AxelorCsrfMatcher csrfMatcher, DefaultCallbackClientFinder clientFinder) {
    this.csrfMatcher = csrfMatcher;
    setProfileManagerFactory(ShiroProfileManager::new);
    setClientFinder(clientFinder);
  }

  @Override
  public Object perform(
      JEEContext context,
      Config config,
      HttpActionAdapter<Object, JEEContext> httpActionAdapter,
      String inputDefaultUrl,
      Boolean inputSaveInSession,
      Boolean inputMultiProfile,
      Boolean inputRenewSession,
      String client) {

    try {
      context.getNativeRequest().setCharacterEncoding("UTF-8");
    } catch (UnsupportedEncodingException e) {
      logger.error(e.getMessage(), e);
    }

    return super.perform(
        context,
        config,
        httpActionAdapter,
        inputDefaultUrl,
        inputSaveInSession,
        inputMultiProfile,
        inputRenewSession,
        client);
  }

  @Override
  protected HttpAction redirectToOriginallyRequestedUrl(JEEContext context, String defaultUrl) {

    // Add CSRF token cookie and header
    csrfMatcher.addResponseCookieAndHeader(context);

    // If XHR, return status code only
    if (AuthPac4jInfo.isXHR(context)) {
      return new OkAction(context.getRequestContent());
    }

    @SuppressWarnings("unchecked")
    final SessionStore<JEEContext> sessionStore = context.getSessionStore();

    final String requestedUrl =
        sessionStore
            .get(context, Pac4jConstants.REQUESTED_URL)
            .filter(WithLocationAction.class::isInstance)
            .map(action -> ((WithLocationAction) action).getLocation())
            .orElse("");

    String redirectUrl = defaultUrl;
    if (StringUtils.notBlank(requestedUrl)) {
      sessionStore.set(context, Pac4jConstants.REQUESTED_URL, null);
      redirectUrl = requestedUrl;
    }

    final String hashLocation =
        context
            .getRequestParameter(AxelorSecurityLogic.HASH_LOCATION_PARAMETER)
            .or(
                () ->
                    sessionStore
                        .get(context, AxelorSecurityLogic.HASH_LOCATION_PARAMETER)
                        .map(String::valueOf))
            .orElse("");
    if (StringUtils.notBlank(hashLocation)) {
      redirectUrl = redirectUrl + hashLocation;
    }

    logger.debug("redirectUrl: {}", redirectUrl);
    return RedirectionActionHelper.buildRedirectUrlAction(context, redirectUrl);
  }
}
