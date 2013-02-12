package com.axelor.web;

import groovy.lang.GString;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import com.axelor.db.Model;
import com.axelor.rpc.Resource;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {

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
	
	static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
		
		@Override
		public void serialize(LocalDateTime value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			if (value != null) {
				String text = value.toDateTime(DateTimeZone.UTC).toString();
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
	
	public ObjectMapperProvider() {
		mapper = new ObjectMapper();
		
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

		SimpleModule module = new SimpleModule("MyModule");
		module.addSerializer(Model.class, new ModelSerializer());
		module.addSerializer(GString.class, new GStringSerializer());
		module.addSerializer(BigDecimal.class, new DecimalSerializer());

		JodaModule jodaModule = new JodaModule();
		jodaModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
		jodaModule.addSerializer(LocalTime.class, new LocalTimeSerializer());

		mapper.registerModule(module);
		mapper.registerModule(jodaModule);
		mapper.registerModule(new GuavaModule());
	}
	
	@Override
	public ObjectMapper getContext(Class<?> objectType) {
		return mapper;
	}
}
