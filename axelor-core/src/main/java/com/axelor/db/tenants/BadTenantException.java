/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.tenants;

public class BadTenantException extends RuntimeException {

  private static final long serialVersionUID = 8196156624484011220L;

  public BadTenantException() {
    super("Tenant identifier from request is invalid or missing");
  }

  public BadTenantException(String message) {
    super(message);
  }

  public BadTenantException(String message, Throwable cause) {
    super(message, cause);
  }
}
