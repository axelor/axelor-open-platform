/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.naming;

import com.axelor.common.StringUtils;
import java.util.Locale;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

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
    final StringBuilder buf = new StringBuilder(text.replace('.', '_'));
    for (int i = 1; i < buf.length() - 1; i++) {
      if (Character.isLowerCase(buf.charAt(i - 1))
          && Character.isUpperCase(buf.charAt(i))
          && Character.isLowerCase(buf.charAt(i + 1))) {
        buf.insert(i++, '_');
      }
    }
    return buf.toString().toLowerCase(Locale.ROOT);
  }
}
