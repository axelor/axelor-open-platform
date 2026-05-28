/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.tenants;

@SuppressWarnings("serial")
public class TenantNotFoundException extends BadTenantException {

  public TenantNotFoundException(String tenant) {
    this(tenant, null);
  }

  public TenantNotFoundException(String tenant, Throwable cause) {
    super("No such tenant: " + tenant, cause);
  }
}
