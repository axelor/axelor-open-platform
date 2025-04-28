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
package com.axelor.quartz;

import com.axelor.common.ResourceUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.internal.DBHelper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;
import org.quartz.SchedulerException;
import org.quartz.utils.HikariCpPoolingConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AxelorConnectionProvider extends HikariCpPoolingConnectionProvider {

  // Creation script path prefix
  private static final String SCRIPT_PATH_PREFIX = "org/quartz/impl/jdbcjobstore/";

  // Default table prefix
  // Can't be changed because tables_*.sql creation scripts have it hardcoded.
  private static final String TABLE_PREFIX = "qrtz_";

  private static final Logger log = LoggerFactory.getLogger(AxelorConnectionProvider.class);

  // {@link StdSchedulerFactory} instantiates this using the default constructor.
  public AxelorConnectionProvider() throws SchedulerException, SQLException {
    super(getInitProperties());

    var dataSource = getDataSource();

    var dataSourceName = DBHelper.getDataSourceName();
    if (StringUtils.notBlank(dataSourceName)) {
      dataSource.setDataSourceJNDI(dataSourceName);
    }

    dataSource.setAutoCommit(false);
  }

  private static Properties getInitProperties() {
    var props = new Properties();

    props.setProperty(DB_DRIVER, DBHelper.getJdbcDriver());
    props.setProperty(DB_URL, DBHelper.getJdbcUrl());
    props.setProperty(DB_USER, DBHelper.getJdbcUser());
    props.setProperty(DB_PASSWORD, DBHelper.getJdbcPassword());

    return props;
  }

  @Override
  public void initialize() throws SQLException {
    super.initialize();

    // Create Quartz tables if they don't exist.
    try (var connection = getConnection()) {
      if (tableExists(connection, TABLE_PREFIX + "%")) {
        log.trace("Quartz tables already exist.");
        return;
      }

      createQuartzTables(connection);
      log.info("Created Quartz tables");
    }
  }

  private boolean tableExists(Connection connection, String tableNamePattern) throws SQLException {
    var metaData = connection.getMetaData();
    try (var resultSet = metaData.getTables(null, null, tableNamePattern, new String[] {"TABLE"})) {
      return resultSet.next();
    }
  }

  private void createQuartzTables(Connection connection) throws SQLException {
    var scriptName = determineScriptName();
    var scriptPath = SCRIPT_PATH_PREFIX + scriptName;

    try (var scriptStream = ResourceUtils.getResourceStream(scriptPath)) {
      if (scriptStream == null) {
        throw new IOException("Quartz schema script not found: " + scriptPath);
      }

      var scriptContent = new String(scriptStream.readAllBytes(), StandardCharsets.UTF_8);
      executeScript(connection, scriptContent);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String determineScriptName() throws SQLException {
    if (DBHelper.isPostgreSQL()) {
      return "tables_postgres.sql";
    } else if (DBHelper.isMySQL()) {
      return "tables_mysql.sql";
    } else if (DBHelper.isOracle()) {
      // Assuming Oracle 23+ for now
      return "tables_oracle23.sql";
    } else if (DBHelper.isHSQL()) {
      return "tables_hsqldb.sql";
    }

    throw new SQLException("Unsupported database type: " + DBHelper.getJdbcDriver());
  }

  private void executeScript(Connection connection, String scriptContent) throws SQLException {
    // Simple split into statements (fine for simple scripts).
    // Also filter out any commit statements, as we commit the changes ourselves.
    var statements =
        Arrays.stream(scriptContent.split(";"))
            .map(String::trim)
            .filter(s -> !s.isEmpty() && !s.startsWith("--") && !"commit".equalsIgnoreCase(s))
            .toList();

    try (var statement = connection.createStatement()) {
      for (var sql : statements) {
        statement.addBatch(sql);
      }
      statement.executeBatch();
      connection.commit();
    } catch (SQLException e) {
      try {
        connection.rollback();
      } catch (SQLException rbEx) {
        e.addSuppressed(rbEx);
      }
      throw e;
    }
  }
}
