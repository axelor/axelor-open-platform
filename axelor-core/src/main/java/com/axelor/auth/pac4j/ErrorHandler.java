/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.common.UriBuilder;
import jakarta.inject.Singleton;
import java.lang.invoke.MethodHandles;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.util.HttpActionHelper;
import org.pac4j.core.util.Pac4jConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ErrorHandler {

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public Object handleException(
      Exception e, HttpActionAdapter httpActionAdapter, WebContext context) {
    if (logger.isDebugEnabled()) {
      logger.debug(e.getMessage(), e);
    } else {
      logger.error(e.getMessage());
    }

    if (httpActionAdapter == null || context == null) {
      throw runtimeException(e);
    }

    if (e instanceof HttpAction action) {
      logger.debug("extra HTTP action required in security: {}", action.getCode());
      return httpActionAdapter.adapt(action, context);
    }

    final var errorUriBuilder =
        UriBuilder.from(AppSettings.get().getBaseURL()).addQueryParam("error", null);
    context
        .getRequestParameter(Pac4jConstants.DEFAULT_CLIENT_NAME_PARAMETER)
        .ifPresent(
            client ->
                errorUriBuilder.addQueryParam(
                    Pac4jConstants.DEFAULT_CLIENT_NAME_PARAMETER, client));
    final String errorUrl = errorUriBuilder.toUri().toString();

    final HttpAction action = HttpActionHelper.buildRedirectUrlAction(context, errorUrl);
    return httpActionAdapter.adapt(action, context);
  }

  protected RuntimeException runtimeException(final Exception exception) {
    if (exception instanceof RuntimeException runtimeException) {
      throw runtimeException;
    } else {
      throw new RuntimeException(exception);
    }
  }
}
