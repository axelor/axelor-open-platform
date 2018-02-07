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
package com.axelor.meta.db.repo;

import java.util.Map;
import java.util.Objects;

import com.axelor.db.EntityHelper;
import com.axelor.db.JpaRepository;
import com.axelor.db.Query;
import com.axelor.db.hibernate.type.JsonFunction;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaJsonModel;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.rpc.Context;
import com.axelor.rpc.JsonContext;
import com.axelor.rpc.Resource;

/**
 * Repository implementation to handle custom model records.
 *
 */
public class MetaJsonRecordRepository extends JpaRepository<MetaJsonRecord> {

	/**
	 * Create new instance of the {@link MetaJsonRecordRepository}.
	 * 
	 */
	public MetaJsonRecordRepository() {
		super(MetaJsonRecord.class);
	}

	/**
	 * Create a {@link Context} for the given json model to seamlessly work with
	 * json fields.
	 * 
	 * <p>
	 * For example:
	 * 
	 * <pre>
	 * Context person = repo.create("Person");
	 * person.put("name", "Some NAME");
	 * person.put("email", "some.name@gmail.com");
	 * 
	 * MetaJsonRecord saved = repo.save(person);
	 * </pre>
	 * 
	 * @param jsonModel
	 *            the name of the json model
	 * @return a {@link Context}
	 */
	public Context create(String jsonModel) {
		Objects.requireNonNull(jsonModel, "jsonModel cannot be null");
		final MetaJsonRecord record = new MetaJsonRecord();
		record.setJsonModel(jsonModel);
		return create(record);
	}

	public Context create(MetaJsonRecord record) {
		Objects.requireNonNull(record, "record cannot be null");
		Objects.requireNonNull(record.getJsonModel(), "jsonModel cannot be null");
		final MetaJsonContext context = new MetaJsonContext(record);
		context.put("jsonModel", record.getJsonModel());
		context.addChangeListener(evt -> record.setAttrs((String) context.get("attrs")));
		return context;
	}

	/**
	 * Create a new {@link MetaJsonRecord} for the given json model with given
	 * values.
	 * 
	 * @param jsonModel
	 *            the name of the json model
	 * @param values
	 *            the values to set
	 * @return a new unsaved instance of {@link MetaJsonRecord}
	 */
	public MetaJsonRecord create(String jsonModel, Map<String, Object> values) {
		final Context context = create(jsonModel);
		if (values != null) {
			context.putAll(values);
		}
		return EntityHelper.getEntity(context.asType(MetaJsonRecord.class));
	}

	/**
	 * Save the json record backed by the given context.
	 * 
	 * @param context
	 *            the json record context
	 * @return saved instance {@link MetaJsonRecord}
	 */
	public MetaJsonRecord save(Context context) {
		if (context instanceof MetaJsonContext && ((MetaJsonContext) context).record != null) {
			return save(((MetaJsonContext) context).record);
		}
		return save(context.asType(MetaJsonRecord.class));
	}
	
	@Override
	public MetaJsonRecord save(MetaJsonRecord entity) {
		final MetaJsonModelRepository models = Beans.get(MetaJsonModelRepository.class);
		final MetaJsonModel model = models.findByName(entity.getJsonModel());
		// set name value
		if (model != null && model.getNameField() != null) {
			entity.setName((String) new JsonContext(entity).get(model.getNameField()));
		}
		return super.save(entity);
	}

	/**
	 * Create a {@link Query} for the given json model.
	 * 
	 * @param jsonModel
	 *            name of the json model
	 * @return an instance of {@link MetaJsonRecordQuery}
	 */
	public MetaJsonRecordQuery all(String jsonModel) {
		return new MetaJsonRecordQuery(jsonModel);
	}
	
	private static class MetaJsonContext extends Context {
		
		private final MetaJsonRecord record;

		public MetaJsonContext(MetaJsonRecord record) {
			super(Resource.toMap(record), MetaJsonRecord.class);
			this.record = record;
		}
	}

	public static class MetaJsonRecordQuery extends Query<MetaJsonRecord> {
		
		private final String jsonModel;

		public MetaJsonRecordQuery(String jsonModel) {
			super(MetaJsonRecord.class);
			this.jsonModel = jsonModel;
		}

		@Override
		public Query<MetaJsonRecord> filter(String filter, Object... params) {
			try {
				return super.filter("(self.jsonModel = :jsonModel) AND (" + filter + ")", params);
			} finally {
				bind("jsonModel", jsonModel);
			}
		}

		public Query<MetaJsonRecord> by(String field, String operator, Object value) {
			final String path = field.startsWith("attrs.") ? field : "attrs." + field;
			final JsonFunction func = JsonFunction.fromPath(path);
			final StringBuilder builder = new StringBuilder(func.toString())
					.append(" ").append(operator)
					.append(" ").append(":param");
			return filter(builder.toString()).bind("param", value);
		}

		public Query<MetaJsonRecord> by(String field, Object value) {
			return by(field, "=", value);
		}
	}
}
