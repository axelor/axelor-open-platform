/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.db.hibernate.dialect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.persistence.PersistenceException;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

public abstract class AbstractJsonExtractFunction implements SQLFunction {

  private static final Pattern NAME_PATTERN = Pattern.compile("\\w+(\\.\\w+)*");
  private static final Pattern ARGS_PATTERN = Pattern.compile("'\\w+'");

  private Type type;

  private String name;

  private String cast;

  public AbstractJsonExtractFunction(String name, Type type, String cast) {
    this.type = type;
    this.name = name;
    this.cast = cast;
  }

  public String getName() {
    return name;
  }

  public String getCast() {
    return cast;
  }

  public Type getType() {
    return type;
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
    return type == null ? firstArgumentType : type;
  }

  protected abstract String transformPath(List<String> path);

  protected String transformFunction(String func) {
    return func;
  }

  private static String validateField(String name) {
    if (NAME_PATTERN.matcher(name).matches()) {
      return name;
    }
    throw new PersistenceException("Invalid field name: " + name);
  }

  private static String validateArg(String name) {
    if (ARGS_PATTERN.matcher(name).matches()) {
      return name;
    }
    throw new PersistenceException("Invalid json field: " + name);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) {
    final StringBuilder buf = new StringBuilder();
    final Iterator iter = arguments.iterator();
    final List<String> path = new ArrayList<>();
    buf.append(getName()).append("(");
    buf.append(validateField((String) iter.next()));
    while (iter.hasNext()) {
      path.add(validateArg((String) iter.next()));
    }
    buf.append(", ");
    buf.append(transformPath(path));
    buf.append(")");
    final String func = transformFunction(buf.toString());
    return cast == null ? func : String.format("cast(%s as %s)", func, cast);
  }
}
