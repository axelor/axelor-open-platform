/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
          "Database dialect: {}, version: {}",
          dialect.getClass().getSimpleName(),
          dialect.getVersion());
    }

    return dialect;
  }

  private Dialect findDialect(DialectResolutionInfo info) {
    final String databaseName = info.getDatabaseName();

    if (TargetDatabase.POSTGRESQL.equals(databaseName)) {
      if (info.isSameOrAfter(12)) {
        return new AxelorPostgreSQLDialect(info);
      }

      log.error("PostgreSQL 12 or later is required.");
      return null;
    }

    if (TargetDatabase.MYSQL.equals(databaseName)) {
      if (info.isSameOrAfter(8)) {
        return new AxelorMySQLDialect(info);
      }

      log.error("MySQL 8.0 or later is required.");
      return null;
    }

    if (TargetDatabase.ORACLE.equals(databaseName)) {
      if (info.isSameOrAfter(19)) {
        return new AxelorOracleDialect(info);
      }
      log.error("Oracle 19 or later is required.");
      return null;
    }

    if (TargetDatabase.HSQLDB.equals(databaseName)) {
      return new AxelorHSQLDialect(info);
    }

    log.error("{} {} is not supported.", databaseName, info);
    return null;
  }
}
