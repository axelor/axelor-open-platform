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
