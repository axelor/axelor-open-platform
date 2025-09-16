/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.dialect.function;

import java.util.stream.Collectors;
import org.hibernate.type.BasicTypeReference;

public class OracleJsonExtractFunction extends AbstractJsonExtractFunction {

  public OracleJsonExtractFunction(BasicTypeReference<?> type, String cast) {
    super("json_value", type, cast, null, Collectors.joining(".", "'$.", "'"));
  }
}
