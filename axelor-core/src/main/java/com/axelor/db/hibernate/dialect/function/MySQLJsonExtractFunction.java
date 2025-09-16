/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.dialect.function;

import java.util.stream.Collectors;
import org.hibernate.type.BasicTypeReference;

public class MySQLJsonExtractFunction extends AbstractJsonExtractFunction {

  public MySQLJsonExtractFunction(BasicTypeReference<?> type, String cast) {
    super("json_extract", type, cast, "json_unquote", Collectors.joining(".", "'$.", "'"));
  }
}
