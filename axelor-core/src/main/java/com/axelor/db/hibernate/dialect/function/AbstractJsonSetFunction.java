/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
import java.util.regex.Pattern;
import javax.persistence.PersistenceException;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

public abstract class AbstractJsonSetFunction implements SQLFunction {

  private static final Pattern PATH_PATTERN = Pattern.compile("\\w+(\\.\\w+)*");

  private String name;

  public AbstractJsonSetFunction(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean hasArguments() {
    return true;
  }

  @Override
  public boolean hasParenthesesIfNoArguments() {
    return true;
  }

  @Override
  public Type getReturnType(Type firstArgumentType, Mapping mapping) throws QueryException {
    return firstArgumentType;
  }

  protected abstract String transformPath(String path);

  protected abstract Object transformValue(Object value);

  private static String validatePath(String name) {
    if (PATH_PATTERN.matcher(name).matches()) {
      return name;
    }
    throw new PersistenceException("Invalid json path: " + name);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) {
    if (arguments.size() != 3) {
      throw new PersistenceException("Invalid use of 'json_set', requires 3 arguments.");
    }

    final StringBuilder buf = new StringBuilder();
    final String field = (String) arguments.get(0);
    final String path = (String) arguments.get(1);
    final Object value = arguments.get(2);

    buf.append(getName()).append("(");
    buf.append(validatePath(field)).append(", ");
    buf.append(transformPath(validatePath(path.substring(1, path.length() - 1)))).append(", ");
    buf.append(transformValue(value));
    buf.append(")");

    return buf.toString();
  }
}
