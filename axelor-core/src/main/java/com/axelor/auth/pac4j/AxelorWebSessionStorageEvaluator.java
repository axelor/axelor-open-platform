/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.mgt.DefaultWebSessionStorageEvaluator;
import org.apache.shiro.web.util.WebUtils;

/**
 * Session storage evaluator
 *
 * <p>Session-less clients disable session creation for the request. However, {@link
 * DefaultWebSessionStorageEvaluator} still allows storing the subject state in a session that
 * already exists, such as when the request also carries a session id cookie. This would
 * authenticate that session with the session-less client credentials without renewing its id.
 *
 * <p>This implementation disables session storage whenever session creation is disabled, so that
 * session-less logins only apply to the current request and leave any existing session untouched.
 */
public class AxelorWebSessionStorageEvaluator extends DefaultWebSessionStorageEvaluator {

  /**
   * Checks whether the subject state may be stored in its session.
   *
   * @param subject the subject for which session state persistence may be enabled
   * @return {@code false} if session creation is disabled for the current request, otherwise the
   *     default behavior
   */
  @Override
  public boolean isSessionStorageEnabled(Subject subject) {
    return WebUtils.isSessionCreationEnabled(subject) && super.isSessionStorageEnabled(subject);
  }
}
