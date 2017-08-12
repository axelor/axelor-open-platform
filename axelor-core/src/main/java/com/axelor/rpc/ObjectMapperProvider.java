/*
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

import static com.axelor.common.StringUtils.isBlank;
import static com.axelor.meta.loader.ModuleManager.isInstalled;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.inject.Singleton;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.Model;
import com.axelor.db.ValueEnum;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaPermissions;
import com.axelor.meta.db.MetaPermissionRule;
import com.axelor.meta.schema.views.AbstractWidget;
import com.axelor.meta.schema.views.Help;
import com.axelor.meta.schema.views.SimpleWidget;
import com.axelor.script.ScriptHelper;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.EnumDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.util.EnumResolver;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import groovy.lang.GString;

@Singleton
public class ObjectMapperProvider implements Provider<ObjectMapper> {

	private final ObjectMapper mapper;

	static class ModelSerializer extends JsonSerializer<Model> {

		@Override
		public void serialize(Model value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			if (value != null) {
				final JsonSerializer<Object> serializer = provider.findValueSerializer(Map.class, null);
				final Map<String, Object> map = Resource.toMap(value);
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

	static class ZonedDateTimeSerializer extends JsonSerializer<java.time.ZonedDateTime> {

		@Override
		public void serialize(java.time.ZonedDateTime value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			if (value != null) {
				jgen.writeString(value.withZoneSameInstant(ZoneOffset.UTC).toString());
			}
		}
	}

	static class LocalDateTimeSerializer extends JsonSerializer<java.time.LocalDateTime> {

		@Override
		public void serialize(java.time.LocalDateTime value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			if (value != null) {
				jgen.writeString(value.atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toString());
			}
		}
	}

	static class LocalTimeSerializer extends JsonSerializer<java.time.LocalTime> {

		@Override
		public void serialize(java.time.LocalTime value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			if (value != null) {
				jgen.writeString(value.format(DateTimeFormatter.ofPattern("HH:mm")));
			}
		}
	}

	static class DecimalSerializer extends JsonSerializer<BigDecimal> {

		@Override
		public void serialize(BigDecimal value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			if (value != null) {
				jgen.writeString(value.toPlainString());
			}
		}
	}

	static class WidgetListSerializer extends JsonSerializer<List<AbstractWidget>> {

		private boolean test(AbstractWidget widget) {

			final String module = widget.getModuleToCheck();
			final String condition = widget.getConditionToCheck();
			if (!isBlank(module) && !isInstalled(module)) {
				return false;
			}
			if (isBlank(condition)) {
				return true;
			}

			final Request request = Request.current();
			if (request == null) {
				return true;
			}

			final ScriptHelper helper = request.getScriptHelper();
			return helper.test(condition);
		}

		private boolean hasAccess(AbstractWidget widget) {
			if (!(widget instanceof SimpleWidget)) {
				return true;
			}
			final SimpleWidget item = (SimpleWidget) widget;
			final String object = item.getModel();
			final String field = item.getName();
			final User user = AuthUtils.getUser();
			if (user == null) {
				return true;
			}
			if (widget instanceof Help && user.getNoHelp() == Boolean.TRUE) {
				return false;
			}
			if (AuthUtils.isAdmin(user) || isBlank(object) || isBlank(field)) {
				return true;
			}
			final MetaPermissions perms = Beans.get(MetaPermissions.class);
			final MetaPermissionRule rule = perms.findRule(user, object, field);
			if (rule == null) {
				return true;
			}

			if (item.getReadonlyIf() == null && rule.getReadonlyIf() != null) {
				item.setReadonlyIf(rule.getReadonlyIf());
			}
			if (item.getHideIf() == null && rule.getHideIf() != null) {
				item.setHideIf(rule.getHideIf());
			}
			if (rule.getCanWrite() != Boolean.TRUE) {
				item.setReadonly(true);
				if (isBlank(rule.getReadonlyIf())) {
					item.setReadonlyIf(null);
				}
			}

			return rule.getCanRead() == Boolean.TRUE;
		}

		@Override
		public void serialize(List<AbstractWidget> value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {

			if (value == null) {
				return;
			}

			jgen.writeStartArray();

			for (AbstractWidget widget : value) {
				if (test(widget) && hasAccess(widget)) {
					jgen.writeObject(widget);
				}
			}

			jgen.writeEndArray();
		}
	}

	static class ListSerializerModifier extends BeanSerializerModifier {

		private WidgetListSerializer listSerializer = new WidgetListSerializer();

		@Override
		public JsonSerializer<?> modifyCollectionSerializer(SerializationConfig config, CollectionType valueType,
				BeanDescription beanDesc, JsonSerializer<?> serializer) {

			if (AbstractWidget.class.isAssignableFrom(valueType.getContentType().getRawClass())) {
				return listSerializer;
			}

			return serializer;
		}
	}
	
	static class EnumFieldSerializer<T extends Enum<?>> extends JsonSerializer<T> {

		@Override
		public void serialize(T value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException, JsonProcessingException {
			if (value == null) {
				return;
			}
			if (value instanceof ValueEnum<?>) {
				gen.writeObject(((ValueEnum<?>) value).getValue());
			} else {
				gen.writeString(value.name());
			}
		}
	}

	static class EnumFieldDeserializer  extends JsonDeserializer<Enum<?>> implements ContextualDeserializer {

		private Class<?> enumType;

		@Override
		@SuppressWarnings("unchecked")
		public Enum<?> deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			if (ValueEnum.class.isAssignableFrom(enumType)) {
				return ValueEnum.of(enumType.asSubclass(ValueEnum.class), p.getCurrentToken().isNumeric()
						? p.getValueAsInt()
						: p.getValueAsString());
			}
			return Enum.valueOf(enumType.asSubclass(Enum.class), p.getValueAsString());
		}

		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
				throws JsonMappingException {
			final JavaType javaType = property == null ? ctxt.getContextualType() : property.getType();
			final Class<?> type = javaType.getRawClass();
			if (ValueEnum.class.isAssignableFrom(type)) {
				EnumFieldDeserializer deser = new EnumFieldDeserializer();
				deser.enumType = type;
				return deser;
			}
			return new EnumDeserializer(EnumResolver.constructUnsafe(type, ctxt.getAnnotationIntrospector()));
		}
	}

	static ObjectMapper createObjectMapper() {
		return createObjectMapper(new ModelSerializer());
	}

	static ObjectMapper createObjectMapper(JsonSerializer<Model> modelSerializer) {

		final ObjectMapper mapper = new ObjectMapper();

		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

		SimpleModule module = new SimpleModule("MyModule");
		module.addSerializer(Model.class, modelSerializer);
		module.addSerializer(GString.class, new GStringSerializer());
		module.addSerializer(BigDecimal.class, new DecimalSerializer());
		
		module.addSerializer(Enum.class, new EnumFieldSerializer<>());
		module.addDeserializer(Enum.class, new EnumFieldDeserializer());

		module.setSerializerModifier(new ListSerializerModifier());

		JavaTimeModule javaTimeModule = new JavaTimeModule();

		javaTimeModule.addSerializer(java.time.ZonedDateTime.class, new ZonedDateTimeSerializer());
		javaTimeModule.addSerializer(java.time.LocalDateTime.class, new LocalDateTimeSerializer());
		javaTimeModule.addSerializer(java.time.LocalTime.class, new LocalTimeSerializer());

		mapper.registerModule(module);
		mapper.registerModule(javaTimeModule);
		mapper.registerModule(new Jdk8Module());
		mapper.registerModule(new ParameterNamesModule());
		mapper.registerModule(new GuavaModule());

		return mapper;
	}

	public ObjectMapperProvider() {
		this.mapper = createObjectMapper();
	}

	@Override
	public ObjectMapper get() {
		return mapper;
	}
}
