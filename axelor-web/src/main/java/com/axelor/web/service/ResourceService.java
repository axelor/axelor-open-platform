/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.web.service;

import java.lang.reflect.Type;

import javax.ws.rs.PathParam;

import com.axelor.db.Model;
import com.axelor.rpc.Resource;
import com.google.inject.Key;
import com.google.inject.util.Types;

public abstract class ResourceService extends AbstractService {

	@PathParam("model")
	private String entity;
	
	protected String getModel() {
		return entity;
	}
	
	@SuppressWarnings("unchecked")
	final protected Class<? extends Model> entityClass() {
		try {
			return (Class<Model>) Class.forName(entity);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	final protected Resource<?> getResource() {
		Type type = Types.newParameterizedType(Resource.class, entityClass());
		return (Resource<?>) getInjector().getInstance(Key.get(type));
	}
}
