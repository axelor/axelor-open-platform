/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.rpc;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.script.SimpleBindings;

import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.Model;
import com.axelor.db.mapper.Adapter;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaJsonRecord;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JsonContext extends SimpleBindings {
	
	static class ModelSerializer extends JsonSerializer<Model> {

		@Override
		public void serialize(Model value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			if (value != null) {
				final JsonSerializer<Object> serializer = provider.findValueSerializer(Map.class, null);
				final Map<String, Object> map = Resource.toMapCompact(value);
				map.remove("$version");
				serializer.serialize(map, jgen, provider);
			}
		}
	}

	private static final ObjectMapper jsonMapper = ObjectMapperProvider.createObjectMapper(new ModelSerializer());

	static {
		jsonMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
	}

	private final String jsonField;
	private final Map<String, Object> fields;
	private final Context context;

	public JsonContext(Context context, Property property, String jsonValue) {
		super(fromJson(jsonValue));
		this.context = context;
		this.jsonField = property.getName();
		this.fields = findFields();
	}

	public JsonContext(MetaJsonRecord record) {
		this(Objects.requireNonNull(record).getJsonModel(), record.getAttrs());
	}

	public JsonContext(String jsonModel, String jsonValue) {
		this(new Context(MetaJsonRecord.class),
				Mapper.of(MetaJsonRecord.class).getProperty(Context.KEY_JSON_ATTRS),
				jsonValue);
	}

	private Map<String, Object> findFields() {
		String jsonModel = (String) context.get(Context.KEY_JSON_MODEL);
		if (jsonModel == null) {
			jsonModel = (String) super.get(Context.KEY_JSON_MODEL);
		}
		if (!StringUtils.isBlank(jsonModel) && MetaJsonRecord.class.isAssignableFrom(context.getContextClass())) {
			return MetaStore.findJsonFields(jsonModel);
		}
		return MetaStore.findJsonFields(context.getContextClass().getName(), jsonField);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> fromJson(String text) {
		try {
			return jsonMapper.readValue(text, Map.class);
		} catch (Exception e) {
			return new HashMap<>();
		}
	}

	private static String toJson(Map<String, Object> value) {
		try {
			return jsonMapper.writeValueAsString(value);
		} catch (Exception e) {
			return null;
		}
	}

	private void ensureManaged(Object value) {
		if (value instanceof Model) {
			final Model bean = (Model) value;
			if (bean.getId() == null || bean.getId() <= 0) {
				throw new IllegalArgumentException();
			}
		}
		if (value instanceof Collection) {
			((Collection<?>) value).forEach(this::ensureManaged);
		}
	}

	private void propagate() {
		context.put(jsonField, toJson(this));
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object get(Object key) {
		final Map<String, Object> field = (Map<String, Object>) fields.get(key);
		if (field == null) {
			return super.get(key);
		}

		final String type = (String) field.getOrDefault("type", "");
		final Object value = super.get(key);
		
		if (value == null || ObjectUtils.isEmpty(value)) {
			return value;
		}

		String target = null;

		switch (type) {
		case "many-to-one":
		case "one-to-many":
		case "many-to-many":
			target = (String) field.get("target");
			break;
		case "json-many-to-one":
		case "json-one-to-many":
		case "json-many-to-many":
			target = (String) field.get("jsonTarget");
			break;
		case "datetime":
			return Adapter.adapt(value, LocalDateTime.class, null, null);
		case "date":
			return Adapter.adapt(value, LocalDate.class, null, null);
		case "decimal":
			return Adapter.adapt(value, BigDecimal.class, null, null);
		case "integer":
			return Adapter.adapt(value, Integer.class, null, null);
		case "boolean":
			return Adapter.adapt(value, Boolean.class, null, null);
		}

		if (target == null) {
			return super.get(key);
		}

		Class<?> targetClass;
		try {
			targetClass = Class.forName(target);
		} catch (ClassNotFoundException e) {
			return super.get(key);
		}
		
		if (value instanceof Map) {
			return ContextProxy.create((Map) value, targetClass);
		}

		if (value instanceof Collection) {
			return ((Collection<?>) value)
				.stream()
				.map(item -> (Map<String, Object>) item)
				.map(item -> ContextProxy.create(item, targetClass))
				.collect(Collectors.toList());
		}

		return super.get(key);
	}

	@Override
	public Object put(String name, Object value) {
		try {
			ensureManaged(value);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("cannot set unsaved values to field: " + name);
		}
		try {
			return super.put(name, value);
		} finally {
			propagate();
		}
	}

	@Override
	public Object remove(Object key) {
		try {
			return super.remove(key);
		} finally {
			propagate();
		}
	}
}
