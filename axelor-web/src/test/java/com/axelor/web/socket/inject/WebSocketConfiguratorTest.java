/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2026 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.web.socket.inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.app.internal.AppFilter;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class WebSocketConfiguratorTest {

  private static final String BASE_URL = "https://app.example.com/webapp";

  @AfterEach
  public void tearDown() throws Exception {
    setBaseURL(null);
  }

  @SuppressWarnings("unchecked")
  private static void setBaseURL(String url) throws Exception {
    final Field field = AppFilter.class.getDeclaredField("BASE_URL");
    field.setAccessible(true);
    final ThreadLocal<String> baseUrl = (ThreadLocal<String>) field.get(null);
    if (url == null) {
      baseUrl.remove();
    } else {
      baseUrl.set(url);
    }
  }

  private boolean isSameOrigin(String origin) {
    return WebSocketConfigurator.isSameOrigin(origin, BASE_URL);
  }

  @Test
  public void testSameOrigin() {
    assertTrue(isSameOrigin("https://app.example.com"));
  }

  @Test
  public void testSameOriginIgnoresCase() {
    assertTrue(isSameOrigin("HTTPS://APP.EXAMPLE.COM"));
  }

  @Test
  public void testSameOriginWithExplicitDefaultPort() {
    assertTrue(isSameOrigin("https://app.example.com:443"));
    assertTrue(WebSocketConfigurator.isSameOrigin("http://localhost", "http://localhost:80"));
  }

  @Test
  public void testCrossOriginIsRejected() {
    assertFalse(isSameOrigin("https://evil.com"));
    assertFalse(isSameOrigin("https://app.example.com.evil.com"));
    assertFalse(isSameOrigin("null"));
  }

  @Test
  public void testDifferentSchemeIsRejected() {
    assertFalse(isSameOrigin("http://app.example.com"));
  }

  @Test
  public void testDifferentPortIsRejected() {
    assertFalse(isSameOrigin("https://app.example.com:8443"));
  }

  @Test
  public void testUnknownBaseUrlRejects() {
    assertFalse(WebSocketConfigurator.isSameOrigin("https://app.example.com", null));
    assertFalse(WebSocketConfigurator.isSameOrigin("https://app.example.com", ""));
    assertFalse(WebSocketConfigurator.isSameOrigin("https://app.example.com", "not a url"));
  }

  @Test
  public void testTenantOrigin() {
    // the scheme of a tenant base url is inferred from the application mode, not the request
    assertTrue(WebSocketConfigurator.isTenantOrigin("https://t1.example.com", "t1.example.com"));
    assertTrue(WebSocketConfigurator.isTenantOrigin("http://t1.example.com", "t1.example.com"));
    assertTrue(
        WebSocketConfigurator.isTenantOrigin("http://t1.example.com:8080", "t1.example.com:8080"));

    assertFalse(WebSocketConfigurator.isTenantOrigin("https://evil.com", "t1.example.com"));
    assertFalse(
        WebSocketConfigurator.isTenantOrigin("https://t1.example.com:8443", "t1.example.com"));
    assertFalse(WebSocketConfigurator.isTenantOrigin("https://t1.example.com", null));
    assertFalse(WebSocketConfigurator.isTenantOrigin("https://t1.example.com", ""));
  }

  @Test
  public void testCheckOriginAllowsMissingOrigin() {
    final WebSocketConfigurator configurator = new WebSocketConfigurator();

    // native clients don't send an Origin header and are not subject to CSRF
    assertTrue(configurator.checkOrigin(null));
    assertTrue(configurator.checkOrigin(""));
  }

  @Test
  public void testCheckOriginUsesRequestBaseUrl() throws Exception {
    final WebSocketConfigurator configurator = new WebSocketConfigurator();

    setBaseURL("http://localhost:8080/webapp");

    assertTrue(configurator.checkOrigin("http://localhost:8080"));
    assertFalse(configurator.checkOrigin("https://evil.com"));
  }
}
