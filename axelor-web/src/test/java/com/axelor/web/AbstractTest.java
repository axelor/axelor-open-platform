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

import com.axelor.test.WebServer;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class AbstractTest {

  private static WebServer server;

  @BeforeClass
  public static void setUp() {
    if (server == null) {
      server = WebServer.create(new WebTestModule());
    }
    server.start();
  }

  @AfterClass
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
