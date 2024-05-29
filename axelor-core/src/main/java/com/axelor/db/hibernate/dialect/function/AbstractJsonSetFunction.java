/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
import jakarta.persistence.PersistenceException;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.Type;

public abstract class AbstractJsonSetFunction extends StandardSQLFunction {

  private static final Pattern PATH_PATTERN = Pattern.compile("\\w+(\\.\\w+)*");

  public AbstractJsonSetFunction(String name) {
    super(name);
  }

  protected abstract String transformPath(String path);

  protected abstract Object transformValue(Object value);

  private static String validatePath(String name) {
    if (PATH_PATTERN.matcher(name).matches()) {
      return name;
    }
    throw new PersistenceException("Invalid json path: " + name);
  }

//  @Override
//  @SuppressWarnings("rawtypes")
//  public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) {
//    if (arguments.size() != 3) {
//      throw new PersistenceException("Invalid use of 'json_set', requires 3 arguments.");
//    }
//
//    final StringBuilder buf = new StringBuilder();
//    final String field = (String) arguments.get(0);
//    final String path = (String) arguments.get(1);
//    final Object value = arguments.get(2);
//
//    buf.append(getName()).append("(");
//    buf.append(validatePath(field)).append(", ");
//    buf.append(transformPath(validatePath(path.substring(1, path.length() - 1)))).append(", ");
//    buf.append(transformValue(value));
//    buf.append(")");
//
//    return buf.toString();
//  }
}
