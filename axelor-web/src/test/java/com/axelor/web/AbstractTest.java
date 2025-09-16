/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web;

import com.axelor.test.WebServer;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractTest {

  private static WebServer server;

  @BeforeAll
  public static void setUp() {
    if (server == null) {
      server = WebServer.create(new WebTestModule());
    }
    server.start();
  }

  @AfterAll
  public static void tearDown() {
    if (server != null) {
      server.stop();
    }
  }

  protected Invocation.Builder jsonPath(String path) {
    return server
        .target()
        .path(path)
        .request(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON);
  }
}
