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
package com.axelor.db.internal.hibernate.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

public class CustomDialectResolver extends StandardDialectResolver {

	private static final long serialVersionUID = 8211296180497513187L;

	public static final CustomDialectResolver INSTANCE = new CustomDialectResolver();

	@Override
	public Dialect resolveDialect(DialectResolutionInfo info) {
		final String databaseName = info.getDatabaseName();
		if ("PostgreSQL".equals(databaseName)) {
			final int majorVersion = info.getDatabaseMajorVersion();
			final int minorVersion = info.getDatabaseMinorVersion();
			if (majorVersion >= 9 && minorVersion >= 4) {
				return new PostgreSQLDialect();
			}
			throw new RuntimeException("PostgreSQL 9.4 or later is required.");
		}
		return super.resolveDialect(info);
	}
}
