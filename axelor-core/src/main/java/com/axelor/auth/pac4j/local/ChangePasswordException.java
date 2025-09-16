/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import org.pac4j.core.exception.CredentialsException;

public class ChangePasswordException extends CredentialsException {

  private static final long serialVersionUID = -1514519555256616172L;

  public ChangePasswordException() {
    super("");
  }

  public ChangePasswordException(String message) {
    super(message);
  }
}
