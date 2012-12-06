package com.axelor.web;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceWebTest;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;

@GuiceModules(WebTestModule.class)
public abstract class AbstractTest extends GuiceWebTest {
	
	@BeforeClass
	public static void setUp() {
		startServer();
	}
	
	@AfterClass
	public static void tearDown(){
		stopServer();
	}
	
	protected WebResource.Builder jsonPath(String path, String... params) {
		
		MultivaluedMap<String, String> form = new Form();
		
		for(int i=0; i < params.length; i += 2) {
			String param = params[i], value = "";
			try {
				value = params[i+1];
			} catch (IndexOutOfBoundsException e){}
			form.add(param, value);
		}

		return resource().path(path).queryParams(form)
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON);
	}

}
