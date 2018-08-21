/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import org.junit.Test;

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
    request.setData(ImmutableMap.of("firstName", (Object) "John", "lastName", "Teen"));

    Response response = crud("search").post(Entity.json(request), Response.class);

    assertNotNull(response);
    assertNotNull(response.getData());

    assertTrue(response.getData() instanceof List);
    assertTrue(((List<?>) response.getData()).size() > 0);
  }
}
