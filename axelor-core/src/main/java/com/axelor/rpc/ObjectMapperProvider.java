/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

import groovy.lang.GString;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import com.axelor.common.StringUtils;
import com.axelor.db.Model;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.meta.schema.views.AbstractWidget;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;

@Singleton
public class ObjectMapperProvider implements Provider<ObjectMapper> {

	private ObjectMapper mapper;

	static class ModelSerializer extends JsonSerializer<Model> {

		@Override
		public void serialize(Model value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			if (value != null) {
				JsonSerializer<Object> serializer = provider.findValueSerializer(Map.class, null);
				Map<String, Object> map = Resource.toMap(value);
				serializer.serialize(map, jgen, provider);
			}
		}
	}
	
	static class GStringSerializer extends JsonSerializer<GString> {
		
		@Override
		public void serialize(GString value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			if (value != null) {
				jgen.writeString(value.toString());
			}
		}
	}
	
	static class DateTimeSerializer extends JsonSerializer<DateTime> {
		
		@Override
		public void serialize(DateTime value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			if (value != null) {
				String text = value.withZone(DateTimeZone.UTC).toString();
				jgen.writeString(text);
			}
		}
	}

	static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
		
		@Override
		public void serialize(LocalDateTime value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			if (value != null) {
				String text = value.toDateTime().withZone(DateTimeZone.UTC).toString();
				jgen.writeString(text);
			}
		}
	}
	
	static class LocalTimeSerializer extends JsonSerializer<LocalTime> {
		
		@Override
		public void serialize(LocalTime value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			if (value != null) {
				String text = value.toString("HH:mm");
				jgen.writeString(text);
			}
		}
	}
	
	static class DecimalSerializer extends JsonSerializer<BigDecimal> {
		
		@Override
		public void serialize(BigDecimal value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			if (value != null) {
				jgen.writeString(value.toPlainString());
			}
		}
	}

	static class WidgetListSerializer extends JsonSerializer<List<AbstractWidget>> {

		@Override
		public void serialize(List<AbstractWidget> value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			
			if (value == null) {
				return;
			}

			jgen.writeStartArray();
			
			for (AbstractWidget widget : value) {
				String module = widget.getModuleToCheck();
				if (StringUtils.isBlank(module) || ModuleManager.isInstalled(module)) {
					jgen.writeObject(widget);
				}
			}
			
			jgen.writeEndArray();
		}
	}

	static class ListSerializerModifier extends BeanSerializerModifier {

		private WidgetListSerializer listSerializer = new WidgetListSerializer();
		
		@Override
		public JsonSerializer<?> modifyCollectionSerializer(
				SerializationConfig config, CollectionType valueType,
				BeanDescription beanDesc, JsonSerializer<?> serializer) {

			if (AbstractWidget.class.isAssignableFrom(valueType.getContentType().getRawClass())) {
				return listSerializer;
			}

			return serializer;
		}
	}
	
	public ObjectMapperProvider() {
		mapper = new ObjectMapper();
		
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

		SimpleModule module = new SimpleModule("MyModule");
		module.addSerializer(Model.class, new ModelSerializer());
		module.addSerializer(GString.class, new GStringSerializer());
		module.addSerializer(BigDecimal.class, new DecimalSerializer());

		module.setSerializerModifier(new ListSerializerModifier());

		JodaModule jodaModule = new JodaModule();
		jodaModule.addSerializer(DateTime.class, new DateTimeSerializer());
		jodaModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
		jodaModule.addSerializer(LocalTime.class, new LocalTimeSerializer());

		mapper.registerModule(module);
		mapper.registerModule(jodaModule);
		mapper.registerModule(new GuavaModule());
	}

	@Override
	public ObjectMapper get() {
		return mapper;
	}
}
