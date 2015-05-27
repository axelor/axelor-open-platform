/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
package com.axelor.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
