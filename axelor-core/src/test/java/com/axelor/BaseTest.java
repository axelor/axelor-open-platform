package com.axelor;

import java.io.InputStreamReader;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.runner.RunWith;

import com.axelor.db.Fixture;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.test.db.Contact;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(GuiceRunner.class)
@GuiceModules({ MyModule.class })
public abstract class BaseTest {

	@Inject
	protected ObjectMapper objectMapper = new ObjectMapper();

	@Inject
	private Fixture fixture;

	@Before
	public void setUp() {
		if (Contact.all().count() == 0) {
			fixture.load("demo");
		}
	}

	protected InputStreamReader read(String json) {
		return new InputStreamReader(getClass().getClassLoader()
				.getResourceAsStream("META-INF/json/" + json));
	}
	
	protected String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e){
			throw new IllegalArgumentException(e);
		}
	}
	
	protected <T> T fromJson(InputStreamReader reader, Class<T> klass) {
		try {
			return objectMapper.readValue(reader, klass);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	protected <T> T fromJson(String json, Class<T> klass) {
		if (json.endsWith(".js"))
			return fromJson(read(json), klass);
		try {
			return objectMapper.readValue(json, klass);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}
