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
package com.axelor.db.hibernate.naming;

import java.util.Locale;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import com.axelor.common.StringUtils;

public class PhysicalNamingStrategyImpl implements PhysicalNamingStrategy {
	
	public static final PhysicalNamingStrategyImpl INSTANCE = new PhysicalNamingStrategyImpl();

	@Override
	public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return addUnderscores(name);
	}

	@Override
	public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return addUnderscores(name);
	}

	@Override
	public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return addUnderscores(name);
	}

	@Override
	public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return addUnderscores(name);
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return addUnderscores(name);
	}

	static Identifier addUnderscores(Identifier name) {
		if (name == null || StringUtils.isBlank(name.getText())) {
			return null;
		}
		return Identifier.toIdentifier(addUnderscores(name.getText()));
	}

	static String addUnderscores(String text) {
		final StringBuilder buf = new StringBuilder( text.replace('.', '_') );
		for (int i=1; i<buf.length()-1; i++) {
			if (
				Character.isLowerCase(buf.charAt(i-1)) &&
				Character.isUpperCase(buf.charAt(i) ) &&
				Character.isLowerCase(buf.charAt(i+1))
			) {
				buf.insert(i++, '_');
			}
		}
		return buf.toString().toLowerCase(Locale.ROOT);
	}
}
