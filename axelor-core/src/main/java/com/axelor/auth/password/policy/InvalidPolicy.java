/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import java.text.MessageFormat;

/**
 * Represents a password policy violation returned by {@link
 * com.axelor.auth.password.PasswordPolicy#validate}.
 *
 * <p>Carries the raw message key and optional format parameters. Use {@link #getTranslatedMessage}
 * to obtain the final, user-facing string.
 */
public final class InvalidPolicy {
  private String message;
  private Object[] parameters;

  public InvalidPolicy(String message, Object... parameters) {
    this.message = message;
    this.parameters = parameters;
  }

  public String getMessage() {
    return message;
  }

  public Object[] getParameters() {
    return parameters;
  }

  public String getTranslatedMessage() {
    if (StringUtils.isBlank(message)) {
      return null;
    }
    return MessageFormat.format(I18n.get(message), parameters);
  }
}
