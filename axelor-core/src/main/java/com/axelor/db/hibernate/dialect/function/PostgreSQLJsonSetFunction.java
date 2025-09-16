/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.dialect.function;

public class PostgreSQLJsonSetFunction extends AbstractJsonSetFunction {

  public PostgreSQLJsonSetFunction() {
    super("jsonb_set", "to_jsonb");
  }

  @Override
  protected String transformPath(String path) {
    return "'{" + path.replace('.', ',') + "}'";
  }
}
