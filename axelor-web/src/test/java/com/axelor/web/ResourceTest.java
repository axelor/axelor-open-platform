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

	protected WebResource.Builder crud(String... params) {
		return jsonPath("ws/rest/" + model, params);
	}

	protected WebResource.Builder meta(String... params) {
		return jsonPath("ws/meta/" + model, params);
	}

	@Test
	public void testFields() {

		Response response = meta().get(Response.class);

		assertNotNull(response);
		assertNotNull(response.getData());

		assertTrue(response.getData() instanceof Map);

		assertEquals(((Map<?,?>) response.getData()).get("model"), model);
	}

	@Test
	public void testSearch() {

		Request request = new Request();
		request.setData(ImmutableMap.of("firstName", (Object) "John", "lastName", "Teen"));

		Response response = crud().post(Response.class, request);

		assertNotNull(response);
		assertNotNull(response.getData());

		assertTrue(response.getData() instanceof List);
		assertTrue(((List<?>) response.getData()).size() > 0);
	}
}
