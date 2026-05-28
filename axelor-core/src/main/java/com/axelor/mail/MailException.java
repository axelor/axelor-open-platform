/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail;

public class MailException extends Exception {

  private static final long serialVersionUID = 4370214485902946867L;

  public MailException(String message) {
    super(message, null);
  }

  public MailException(Throwable cause) {
    super(cause);
  }

  public MailException(String message, Throwable cause) {
    super(message, cause);
  }
}
