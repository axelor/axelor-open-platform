/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.pac4j.core.context.WebContext;

/**
 * Marker for direct clients that authenticate from the request itself and must never cause a
 * session to be created.
 */
public interface StatelessClient {

  /**
   * Whether this client can extract credentials from the given request.
   *
   * @param context the web context
   * @return {@code true} if the request carries credentials for this client
   */
  boolean hasCredentials(WebContext context);

  /**
   * Prevents any session from being created while handling the current request.
   *
   * @param context the web context
   */
  default void disableSessionCreation(WebContext context) {
    context.setRequestAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED, Boolean.FALSE);
  }
}
