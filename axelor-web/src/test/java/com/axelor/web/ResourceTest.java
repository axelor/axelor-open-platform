/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ResourceTest extends AbstractTest {

  protected String model = "com.axelor.web.db.Contact";

  protected Invocation.Builder crud(String action) {
    String path = "/rest/" + model;
    if (action != null) {
      path = path + "/" + action;
    }
    return jsonPath(path);
  }

  @Test
  public void testFields() {

    Response response = jsonPath("/meta/fields/" + model).get(Response.class);

    assertNotNull(response);
    assertNotNull(response.getData());

    assertTrue(response.getData() instanceof Map);

    assertEquals(((Map<?, ?>) response.getData()).get("model"), model);
  }

  @Test
  public void testSearch() {

    Request request = new Request();
    request.setData(Map.of("firstName", (Object) "John", "lastName", "Teen"));

    Response response = crud("search").post(Entity.json(request), Response.class);

    assertNotNull(response);
    assertNotNull(response.getData());

    assertTrue(response.getData() instanceof List);
    assertTrue(((List<?>) response.getData()).size() > 0);
  }
}
