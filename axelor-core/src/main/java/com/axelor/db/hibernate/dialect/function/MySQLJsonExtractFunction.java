/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.db.hibernate.dialect.function;

import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.type.Type;

public class MySQLJsonExtractFunction extends AbstractJsonExtractFunction {

  public MySQLJsonExtractFunction(Type type, String cast) {
    super("json_extract", type, cast);
  }

  @Override
  public String transformPath(List<String> path) {
    return path.stream()
        .map(item -> item.substring(1, item.length() - 1))
        .collect(Collectors.joining(".", "'$.", "'"));
  }

  @Override
  protected String transformFunction(String func) {
    return String.format("json_unquote(%s)", func);
  }
}
