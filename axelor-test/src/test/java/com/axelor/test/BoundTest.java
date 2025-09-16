/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.inject.AbstractModule;
import com.google.inject.servlet.RequestScoped;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

  @BeforeAll
  public static void beforeClass() {
    server.start();
  }

  @AfterAll
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
