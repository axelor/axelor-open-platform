/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.db.hibernate.dialect.function;

import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.type.Type;

public class OracleJsonExtractFunction extends AbstractJsonExtractFunction {

  public OracleJsonExtractFunction(Type type, String cast) {
    super("json_value", type, cast);
  }

  @Override
  protected String transformPath(List<String> path) {
    return path.stream()
        .map(item -> item.substring(1, item.length() - 1))
        .collect(Collectors.joining(".", "'$.", "'"));
  }
}
