package com.axelor.meta;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;

import org.junit.runner.RunWith;

import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(GuiceRunner.class)
@GuiceModules(TestModule.class)
public abstract class AbstractTest {

	private ObjectMapper objectMapper = new MetaMapper();

	protected InputStream read(String resource) {
		return Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(resource);
	}

	protected ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	protected <T> T unmarshal(String resource, Class<T> type) throws JAXBException {
		return MetaMapper.unmarshal(read(resource), type);
	}

	protected String toJson(Object object) throws JsonGenerationException,
			JsonMappingException, IOException {
		return getObjectMapper()
				.writerWithDefaultPrettyPrinter()
				.writeValueAsString(object);
	}
}
