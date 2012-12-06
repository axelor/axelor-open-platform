package com.axelor.meta;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetaMapper extends ObjectMapper {

	public MetaMapper() {
		configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		setSerializationInclusion(Include.NON_NULL);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T unmarshal(InputStream stream, Class<T> type) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(type);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		return (T) unmarshaller.unmarshal(stream);
	}

	public String toJSON(InputStream stream, Class<?> type) 
			throws JAXBException, JsonGenerationException,
			JsonMappingException, IOException {

		Object value = MetaMapper.unmarshal(stream, type);
		return writeValueAsString(value);
	}
}
