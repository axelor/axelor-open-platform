/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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

    final Dialect dialect = findDialect(info);

    if (dialect != null) {
      log.info(
          "Database engine: {} {}.{}",
          info.getDatabaseName(),
          info.getDatabaseMajorVersion(),
          info.getDatabaseMinorVersion());
      log.debug("Database dialect: {}", dialect);
    }

    return dialect;
  }

  private Dialect findDialect(DialectResolutionInfo info) {
    final String databaseName = info.getDatabaseName();
    final int majorVersion = info.getDatabaseMajorVersion();
    final int minorVersion = info.getDatabaseMinorVersion();

    if (TargetDatabase.POSTGRESQL.equals(databaseName)) {
      if (majorVersion >= 12) {
        return new AxelorPostgreSQL10Dialect(info);
      }

      log.error("PostgreSQL 12 or later is required.");
      return null;
    }

    if (TargetDatabase.MYSQL.equals(databaseName)) {
      if (majorVersion >= 8) {
        return new AxelorMySQL8Dialect(info);
      }

      log.error("MySQL 8.0 or later is required.");
      return null;
    }

    if (TargetDatabase.ORACLE.equals(databaseName)) {
      if (majorVersion >= 19) {
        return new AxelorOracle12cDialect(info);
      }
      log.error("Oracle 19 or later is required.");
      return null;
    }

    if (TargetDatabase.HSQLDB.equals(databaseName)) {
      return new AxelorHSQLDialect();
    }

    log.error("{} {}.{} is not supported.", databaseName, majorVersion, minorVersion);
    return null;
  }
}
