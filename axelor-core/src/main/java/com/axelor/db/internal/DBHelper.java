/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.db.internal;

import static com.axelor.common.StringUtils.isBlank;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.w3c.dom.Document;

import com.axelor.app.AppSettings;
import com.axelor.common.ResourceUtils;

/**
 * This class provides some database helper methods (for internal use only).
 *
 */
public class DBHelper {

	private static Boolean unaccentSupport = null;

	private static final String UNACCENT_CHECK = "SELECT unaccent('text')";
	private static final String UNACCENT_CREATE = "CREATE EXTENSION IF NOT EXISTS unaccent";

	private static final String XPATH_ROOT = "/persistence/persistence-unit[@name='persistenceUnit']";
	private static final String XPATH_ROOT_TEST = "/persistence/persistence-unit[@name='testUnit']";

	private static final String XPATH_NON_JTA_DATA_SOURCE 	= "non-jta-data-source";
	private static final String XPATH_SHARED_CACHE_MODE 	= "shared-cache-mode";

	private static final String XPATH_PERSISTENCE_DRIVER 	= "properties/property[@name='javax.persistence.jdbc.driver']/@value";
	private static final String XPATH_PERSISTENCE_URL 		= "properties/property[@name='javax.persistence.jdbc.url']/@value";
	private static final String XPATH_PERSISTENCE_USER 		= "properties/property[@name='javax.persistence.jdbc.user']/@value";
	private static final String XPATH_PERSISTENCE_PASSWORD 	= "properties/property[@name='javax.persistence.jdbc.password']/@value";

	private static final String CONFIG_DATASOURCE	= "db.default.datasource";
	private static final String CONFIG_DRIVER 		= "db.default.driver";
	private static final String CONFIG_URL 			= "db.default.url";
	private static final String CONFIG_USER 		= "db.default.user";
	private static final String CONFIG_PASSWORD 	= "db.default.password";

	private static String jndiName;
	private static String cacheMode;

	private static String jdbcDriver;
	private static String jdbcUrl;
	private static String jdbcUser;
	private static String jdbcPassword;

	static {
		initialize();
	}

	private DBHelper() {
	}

	private static String evaluate(XPath xpath, String base, String path, Document document) {
		try {
			return xpath.evaluate(base + "/" + path, document).trim();
		} catch (Exception e) {
		}
		return null;
	}

	private static void initialize() {

		final AppSettings settings = AppSettings.get();

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final XPathFactory xpf = XPathFactory.newInstance();
		final XPath xpath = xpf.newXPath();

		jndiName		= settings.get(CONFIG_DATASOURCE);
		jdbcDriver 		= settings.get(CONFIG_DRIVER);
		jdbcUrl 		= settings.get(CONFIG_URL);
		jdbcUser 		= settings.get(CONFIG_USER);
		jdbcPassword 	= settings.get(CONFIG_PASSWORD);

		try (
			final InputStream res = ResourceUtils.getResourceStream("META-INF/persistence.xml")) {
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document document = db.parse(res);

			cacheMode = evaluate(xpath, XPATH_ROOT, XPATH_SHARED_CACHE_MODE, document);

			if (isBlank(jndiName)) {
				jndiName = evaluate(xpath, XPATH_ROOT, XPATH_NON_JTA_DATA_SOURCE, document);
			}

			if (isBlank(jndiName) && isBlank(jdbcDriver)) {
				jdbcDriver		= evaluate(xpath, XPATH_ROOT, XPATH_PERSISTENCE_DRIVER, document);
				jdbcUrl 		= evaluate(xpath, XPATH_ROOT, XPATH_PERSISTENCE_URL, document);
				jdbcUser 		= evaluate(xpath, XPATH_ROOT, XPATH_PERSISTENCE_USER, document);
				jdbcPassword 	= evaluate(xpath, XPATH_ROOT, XPATH_PERSISTENCE_PASSWORD, document);
			}
			if (isBlank(jndiName) && isBlank(jdbcDriver)) {
				jdbcDriver		= evaluate(xpath, XPATH_ROOT_TEST, XPATH_PERSISTENCE_DRIVER, document);
				jdbcUrl 		= evaluate(xpath, XPATH_ROOT_TEST, XPATH_PERSISTENCE_URL, document);
				jdbcUser 		= evaluate(xpath, XPATH_ROOT_TEST, XPATH_PERSISTENCE_USER, document);
				jdbcPassword 	= evaluate(xpath, XPATH_ROOT_TEST, XPATH_PERSISTENCE_PASSWORD, document);
			}

		} catch (Exception e) {
		}
	}

	/**
	 * Get the JDBC connection configured for the application.
	 * <p>
	 * The connection is independent of JPA connection, so use carefully. It
	 * should be used only when JPA context is not available.
	 * </p>
	 *
	 * @return a {@link Connection}
	 * @throws NamingException
	 *             if configured JNDI name can't be resolved
	 * @throws SQLException
	 *             if connection can't be obtained
	 * @throws ClassNotFoundException
	 *             if JDBC driver is not found
	 */
	public static Connection getConnection() throws NamingException, SQLException {

		if (!isBlank(jndiName)) {
			final DataSource ds = (DataSource) InitialContext.doLookup(jndiName);
			return ds.getConnection();
		}

		try {
			Class.forName(jdbcDriver);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
	}

	/**
	 * Run database migration scripts using flyway migration engine.
	 * 
	 */
	public static void migrate() {
		final Flyway flyway = new Flyway();
		if (!isBlank(jndiName)) {
			try {
				flyway.setDataSource((DataSource) InitialContext.doLookup(jndiName));
			} catch (NamingException e) {
				throw new FlywayException(e);
			}
		} else {
			flyway.setDataSource(jdbcUrl, jdbcUser, jdbcPassword);
		}
		flyway.migrate();
	}
	
	public static String getDataSourceName() {
		return jndiName;
	}

	/**
	 * Check whether non-jta data source is used.
	 *
	 */
	public static boolean isDataSourceUsed() {
		return !isBlank(jndiName);
	}

	/**
	 * Check whether shared cache is enabled.
	 *
	 */
	public static boolean isCacheEnabled() {
		if (isBlank(cacheMode)) return false;
		if (cacheMode.equals("ALL")) return true;
		if (cacheMode.equals("ENABLE_SELECTIVE")) return true;
		return false;
	}
	
	/**
	 * Whether using oracle database.
	 * 
	 */
	public static boolean isOracle() {
		return jdbcDriver != null && jdbcDriver.contains("Oracle");
	}

	/**
	 * Check whether the database has unaccent support.
	 *
	 */
	public static boolean isUnaccentEnabled() {
		if (unaccentSupport == null) {
			try {
				unaccentSupport = testUnaccent();
			} catch (Exception e) {
				unaccentSupport = Boolean.FALSE;
			}
		}
		return unaccentSupport == Boolean.TRUE;
	}

	private static boolean testUnaccent() throws Exception {
		Connection connection = getConnection();
		Statement stmt = connection.createStatement();
		try {
			try {
				stmt.executeQuery(UNACCENT_CHECK);
				return true;
			} catch (Exception e) {
			}
			try {
				stmt.executeUpdate(UNACCENT_CREATE);
				return true;
			} catch (Exception e) {
			}
		} finally {
			stmt.close();
			connection.close();
		}
		return false;
	}
}
