/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.test;

import static org.junit.Assert.assertEquals;

import com.google.inject.AbstractModule;
import com.google.inject.servlet.RequestScoped;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class BoundTest {

  static class MyModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(BoundPerRequestResource.class);
      bind(BoundNoScopeResource.class);
      bind(BoundSingletonResource.class);
    }
  }

  @Path("/bound/perrequest")
  @RequestScoped
  public static class BoundPerRequestResource {

    @Context UriInfo ui;

    @QueryParam("x")
    String x;

    @GET
    @Produces("text/plain")
    public String getIt() {
      assertEquals("/bound/perrequest", ui.getPath());
      assertEquals("x", x);

      return "OK";
    }
  }

  @Path("/bound/noscope")
  public static class BoundNoScopeResource {

    @Context UriInfo ui;

    @QueryParam("x")
    String x;

    @GET
    @Produces("text/plain")
    public String getIt() {
      assertEquals("/bound/noscope", ui.getPath());
      assertEquals("x", x);

      return "OK";
    }
  }

  @Path("/bound/singleton")
  @Singleton
  public static class BoundSingletonResource {

    @Context UriInfo ui;

    @GET
    @Produces("text/plain")
    public String getIt() {
      assertEquals("/bound/singleton", ui.getPath());
      String x = ui.getQueryParameters().getFirst("x");
      assertEquals("x", x);

      return "OK";
    }
  }

  private static WebServer server = WebServer.create(new MyModule());

  @BeforeClass
  public static void beforeClass() {
    server.start();
  }

  @AfterClass
  public static void afterClass() {
    server.stop();
  }

  @Test
  public void testBoundPerRequestScope() {
    WebTarget r = server.target().path("/bound/perrequest").queryParam("x", "x");
    String s = r.request().get(String.class);
    assertEquals(s, "OK");
  }

  @Test
  public void testBoundNoScopeResource() {
    WebTarget r = server.target().path("/bound/noscope").queryParam("x", "x");
    String s = r.request().get(String.class);
    assertEquals(s, "OK");
  }

  @Test
  public void testBoundSingletonResourcee() {
    WebTarget r = server.target().path("/bound/singleton").queryParam("x", "x");
    String s = r.request().get(String.class);
    assertEquals(s, "OK");
  }
}
