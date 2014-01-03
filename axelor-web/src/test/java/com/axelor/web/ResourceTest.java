/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
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
