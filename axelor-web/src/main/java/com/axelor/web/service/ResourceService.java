/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.service;

import com.axelor.db.Model;
import com.axelor.rpc.Resource;
import com.google.inject.Key;
import com.google.inject.util.Types;
import jakarta.ws.rs.PathParam;
import java.lang.reflect.Type;

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
