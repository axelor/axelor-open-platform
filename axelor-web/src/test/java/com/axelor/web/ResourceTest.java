package com.axelor.web;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.google.common.collect.ImmutableMap;
import com.sun.jersey.api.client.WebResource;

public class ResourceTest extends AbstractTest {

	protected String model = "com.axelor.web.db.Contact";

	protected WebResource.Builder crud(String action, String... params) {
		String path = "ws/rest/" + model;
		if (action != null) {
			path = path + "/" + action;
		}
		return jsonPath(path, params);
	}

	@Test
	public void testFields() {

		Response response = jsonPath("ws/meta/fields/" + model).get(Response.class);

		assertNotNull(response);
		assertNotNull(response.getData());

		assertTrue(response.getData() instanceof Map);

		assertEquals(((Map<?,?>) response.getData()).get("model"), model);
	}

	@Test
	public void testSearch() {

		Request request = new Request();
		request.setData(ImmutableMap.of("firstName", (Object) "John", "lastName", "Teen"));

		Response response = crud("search").post(Response.class, request);

		assertNotNull(response);
		assertNotNull(response.getData());

		assertTrue(response.getData() instanceof List);
		assertTrue(((List<?>) response.getData()).size() > 0);
	}
}
