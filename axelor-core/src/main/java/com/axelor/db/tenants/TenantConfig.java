/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.tenants;

/**
 * The contract to provide tenant connection settings.
 *
 * <p>The config should provide either JNDI data-source name or JDBC connection properties for the
 * database connection.
 */
public interface TenantConfig {

  static final String DEFAULT_TENANT_ID = "default";

  /** Whether the tenant is active. */
  Boolean getActive();

  /** Whether the tenant is visible in tenant selection box. */
  Boolean getVisible();

  /** The unique tenant identifier. */
  String getTenantId();

  /** The display name. */
  String getTenantName();

  /** Comma separated list of hostnames for which this tenant is valid. */
  String getTenantHosts();

  /** Comma separated list of roles allowed to use this tenant. */
  String getTenantRoles();

  /** JNDI data source name. */
  String getJndiDataSource();

  /** The JDBC driver for the tenant. */
  String getJdbcDriver();

  /** The JDBC connection url for the tenant. */
  String getJdbcUrl();

  /** The JDBC user for the tenant. */
  String getJdbcUser();

  /** The JDBC password for the tenant. */
  String getJdbcPassword();
}
