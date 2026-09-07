/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.socket.inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.app.internal.AppFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class WebSocketConfiguratorTest {

  private static final String BASE_URL = "https://app.example.com/webapp";

  @AfterEach
  public void tearDown() {
    AppFilter.setBaseURL(null);
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
  public void testCheckOriginUsesRequestBaseUrl() {
    final WebSocketConfigurator configurator = new WebSocketConfigurator();

    AppFilter.setBaseURL("http://localhost:8080/webapp");

    assertTrue(configurator.checkOrigin("http://localhost:8080"));
    assertFalse(configurator.checkOrigin("https://evil.com"));
  }
}
