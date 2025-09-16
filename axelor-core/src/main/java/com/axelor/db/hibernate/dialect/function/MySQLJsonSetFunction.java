/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.dialect.function;

public class MySQLJsonSetFunction extends AbstractJsonSetFunction {

  public MySQLJsonSetFunction() {
    super("json_set");
  }

  @Override
  protected String transformPath(String path) {
    return "'$." + path + "'";
  }
}
