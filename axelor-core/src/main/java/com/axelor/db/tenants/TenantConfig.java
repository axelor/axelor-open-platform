/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
