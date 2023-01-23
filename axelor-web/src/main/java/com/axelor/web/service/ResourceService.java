/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.web.service;

import com.axelor.db.Model;
import com.axelor.rpc.Resource;
import com.google.inject.Key;
import com.google.inject.util.Types;
import java.lang.reflect.Type;
import javax.ws.rs.PathParam;

public abstract class ResourceService extends AbstractService {

  @PathParam("model")
  private String entity;

  protected String getModel() {
    return entity;
  }

  @SuppressWarnings("unchecked")
  protected final Class<? extends Model> entityClass() {
    try {
      return (Class<Model>) Class.forName(entity);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }

  protected final Resource<?> getResource() {
    Type type = Types.newParameterizedType(Resource.class, entityClass());
    return (Resource<?>) getInjector().getInstance(Key.get(type));
  }
}
