/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.dialect;

import static org.hibernate.type.SqlTypes.OTHER;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

public class AxelorHSQLDialect extends HSQLDialect {

  public AxelorHSQLDialect(DialectResolutionInfo info) {
    super(info);
  }

  @Override
  protected String columnType(int sqlTypeCode) {
    if (sqlTypeCode == OTHER) {
      return "clob";
    }
    return super.columnType(sqlTypeCode);
  }
}
