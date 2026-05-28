/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import com.axelor.i18n.I18n;
import java.text.MessageFormat;

/**
 * Represents a user-facing description of an active password policy requirement, intended for
 * display as guidance on the login or change-password page before the user submits.
 *
 * <p>Carries the policy ID, a raw translatable message key, and optional format parameters. Use
 * {@link #getTranslatedMessage()} to obtain the final, translated and formatted string.
 *
 * @see com.axelor.auth.password.PasswordPolicy#getDescription()
 */
public final class PolicyDescription {

  private final String policyId;
  private final String message;
  private final Object[] parameters;

  public PolicyDescription(String policyId, String message, Object... parameters) {
    this.policyId = policyId;
    this.message = message;
    this.parameters = parameters;
  }

  /** Returns the identifier of the policy that produced this description. */
  public String getPolicyId() {
    return policyId;
  }

  /** Returns the raw (untranslated) message key. */
  public String getMessage() {
    return message;
  }

  /** Returns the format parameters to interpolate into the translated message. */
  public Object[] getParameters() {
    return parameters;
  }

  /** Returns the translated and formatted description string, ready for display. */
  public String getTranslatedMessage() {
    return MessageFormat.format(I18n.get(message), parameters);
  }
}
