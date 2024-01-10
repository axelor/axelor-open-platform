/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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

import com.axelor.common.UriBuilder;
import java.lang.invoke.MethodHandles;
import javax.inject.Inject;
import javax.inject.Singleton;
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

  private final AuthPac4jInfo pac4jInfo;

  @Inject
  public ErrorHandler(AuthPac4jInfo pac4jInfo) {
    this.pac4jInfo = pac4jInfo;
  }

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

    if (e instanceof HttpAction) {
      final var action = (HttpAction) e;
      logger.debug("extra HTTP action required in security: {}", action.getCode());
      return httpActionAdapter.adapt(action, context);
    }

    final var errorUriBuilder =
        UriBuilder.from(pac4jInfo.getBaseUrl()).addQueryParam("error", null);
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
    if (exception instanceof RuntimeException) {
      throw (RuntimeException) exception;
    } else {
      throw new RuntimeException(exception);
    }
  }
}
