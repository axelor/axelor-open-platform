package com.axelor.db;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;

public class JpaRepository<T extends Model> implements Repository<T> {

	protected Class<T> modelClass;

	public JpaRepository(Class<T> modelClass) {
		this.modelClass = modelClass;
	}

	@Override
	public List<Property> fields() {
		final Property[] fields = JPA.fields(modelClass);
		if (fields == null) {
			return null;
		}
		return Arrays.asList(fields);
	}

	@Override
	public Query<T> all() {
		return JPA.all(modelClass);
	}

	@Override
	public T create(Map<String, Object> values) {
		return Mapper.toBean(modelClass, values);
	}

	@Override
	public T copy(T entity, boolean deep) {
		return JPA.copy(entity, deep);
	}

	@Override
	public T find(Long id) {
		return JPA.find(modelClass, id);
	}

	@Override
	public T save(T entity) {
		return JPA.save(entity);
	}

	@Override
	public void remove(T entity) {
		JPA.remove(entity);
	}
}
