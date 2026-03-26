/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import com.axelor.auth.password.policy.InvalidPolicy;
import com.axelor.i18n.I18n;
import com.axelor.rpc.ResponseException;
import java.text.MessageFormat;

/**
 * Thrown when a password change attempt fails due to a policy violation or other constraint.
 *
 * <p>When constructed with an {@link InvalidPolicy}, {@link #getMessage()} returns a translated,
 * user-facing message combining a generic title with the policy violation detail.
 */
public class ChangePasswordException extends ResponseException {

  private static final long serialVersionUID = -1514519555256616172L;

  private static final String TITLE = /*$$(*/ "Error changing password" /*)*/;
  private static final String BODY_TITLE = /*$$(*/ "Invalid password" /*)*/;

  private InvalidPolicy error = null;

  public ChangePasswordException() {
    this("");
  }

  public ChangePasswordException(String message) {
    super(message, I18n.get(TITLE), null);
  }

  public ChangePasswordException(InvalidPolicy error) {
    super(error.getMessage(), I18n.get(TITLE), null);
    this.error = error;
  }

  @Override
  public String getMessage() {
    if (error != null) {
      return MessageFormat.format("{0} : {1}", I18n.get(BODY_TITLE), error.getTranslatedMessage());
    }
    return super.getMessage();
  }
}
