/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.db.hibernate.dialect;

import com.axelor.db.internal.TargetDatabase;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDialectResolver implements DialectResolver {

  private static final long serialVersionUID = 8211296180497513187L;

  private static final Logger log = LoggerFactory.getLogger(CustomDialectResolver.class);

  public static final CustomDialectResolver INSTANCE = new CustomDialectResolver();

  @Override
  public Dialect resolveDialect(DialectResolutionInfo info) {
    final String databaseName = info.getDatabaseName();
    final int majorVersion = info.getDatabaseMajorVersion();
    final int minorVersion = info.getDatabaseMinorVersion();
    if (TargetDatabase.HSQLDB.equals(databaseName)) {
      return new AxelorHSQLDialect();
    }
    if (TargetDatabase.POSTGRESQL.equals(databaseName)) {
      // Don't support version < 9.4
      if (majorVersion == 9) {
        log.debug(
            "Consider upgrading to 'PostgreSQL >= 10' for better performance and functionality.");
        if (minorVersion == 4) {
          return new AxelorPostgreSQL94Dialect();
        } else if (minorVersion >= 5) {
          return new AxelorPostgreSQL95Dialect();
        }
      } else if (majorVersion >= 10) {
        return new AxelorPostgreSQL10Dialect();
      }

      log.error("PostgreSQL 9.4 or later is required.");
    }
    if (TargetDatabase.ORACLE.equals(databaseName)) {
      if (majorVersion >= 12) {
        return new AxelorOracle12cDialect();
      }
      log.error("Oracle 12c or later is required.");
    }
    if (TargetDatabase.MYSQL.equals(databaseName)) {
      // Don't support version < 5.7
      if (majorVersion == 5) {
        if (minorVersion >= 7) {
          return new AxelorMySQL57Dialect();
        }
      } else if (majorVersion > 5) {
        // There is no MySQL 6 or 7, only MySQL 8.
        return new AxelorMySQL8Dialect();
      }

      log.error("MySQL 5.7 or later is required.");
    }
    log.error("{} {}.{} is not supported.", databaseName, majorVersion, minorVersion);
    return null;
  }
}
