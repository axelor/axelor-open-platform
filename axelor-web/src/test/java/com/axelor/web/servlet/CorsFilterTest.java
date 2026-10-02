/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.servlet;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_MAX_AGE;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static com.google.common.net.HttpHeaders.ORIGIN;
import static jakarta.ws.rs.core.HttpHeaders.HOST;
import static jakarta.ws.rs.core.HttpHeaders.VARY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

public class CorsFilterTest {

  private static final String SERVER_HOST = "axelor.example.com";
  private static final String ALLOWED_ORIGIN = "https://app.example.com";

  /** Recorded outcome of a filtered request. */
  private static class Result {
    Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    int status = HttpServletResponse.SC_OK;
    boolean chained;

    String header(String name) {
      var values = headers.get(name);
      return values == null ? null : String.join(",", values);
    }
  }

  private static Filter filter(String allowOrigin, boolean allowCredentials) {
    return new CorsFilter().configure(allowOrigin, allowCredentials);
  }

  private static Filter filter(String allowOrigin) {
    return new CorsFilter().configure(allowOrigin);
  }

  private static Result request(Filter filter, String method, String origin) {
    Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    headers.put(HOST, SERVER_HOST);

    if (origin != null) {
      headers.put(ORIGIN, origin);
    }

    // Simulate browsers sending Access-Control-Request-Method during preflight OPTIONS.
    if (HttpMethod.OPTIONS.equals(method)) {
      headers.put(ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST);
    }

    var req =
        (HttpServletRequest)
            Proxy.newProxyInstance(
                CorsFilterTest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, m, args) ->
                    switch (m.getName()) {
                      case "getHeader" -> headers.get((String) args[0]);
                      case "getMethod" -> method;
                      case "getContentType" -> null;
                      default -> throw new UnsupportedOperationException(m.getName());
                    });

    var result = new Result();
    var res =
        (HttpServletResponse)
            Proxy.newProxyInstance(
                CorsFilterTest.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (proxy, m, args) -> {
                  switch (m.getName()) {
                    case "setHeader" ->
                        result.headers.put(
                            (String) args[0], new ArrayList<>(List.of((String) args[1])));
                    case "addHeader" ->
                        result
                            .headers
                            .computeIfAbsent((String) args[0], k -> new ArrayList<>())
                            .add((String) args[1]);
                    case "setStatus" -> result.status = (int) args[0];
                    default -> throw new UnsupportedOperationException(m.getName());
                  }
                  return null;
                });

    try {
      filter.doFilter(req, res, (rq, rs) -> result.chained = true);
    } catch (IOException | ServletException e) {
      throw new RuntimeException(e);
    }

    return result;
  }

  private static Result get(Filter filter, String origin) {
    return request(filter, HttpMethod.GET, origin);
  }

  private static Result get(String allowOrigin, boolean allowCredentials, String origin) {
    return get(filter(allowOrigin, allowCredentials), origin);
  }

  @Test
  void testDisabledByDefault() {
    var result = get(null, true, "https://evil.com");
    assertTrue(result.chained);
    assertTrue(result.headers.isEmpty());
  }

  @Test
  void testSameOriginIsNotHandled() {
    var result = get(ALLOWED_ORIGIN, true, "https://" + SERVER_HOST);
    assertTrue(result.chained);
    assertNull(result.header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertEquals(ORIGIN, result.header(VARY));
  }

  @Test
  void testRequestWithoutOriginIsNotHandled() {
    var result = get(ALLOWED_ORIGIN, true, null);
    assertTrue(result.chained);
    assertNull(result.header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertEquals(ORIGIN, result.header(VARY));
  }

  @Test
  void testWildcardSameOriginIsNotHandled() {
    var result = get("*", false, "https://" + SERVER_HOST);
    assertTrue(result.chained);
    assertTrue(result.headers.isEmpty());
  }

  @Test
  void testWildcardSendsLiteralWildcard() {
    var result = get("*", false, "https://evil.com");
    assertTrue(result.chained);
    assertEquals("*", result.header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertNull(result.header(ACCESS_CONTROL_ALLOW_CREDENTIALS));
    assertNull(result.header(VARY));
  }

  @Test
  void testWildcardNeverAllowsCredentials() {
    var filter = filter("*", true);
    for (var method : List.of(HttpMethod.GET, HttpMethod.OPTIONS)) {
      var result = request(filter, method, "https://evil.com");
      assertEquals("*", result.header(ACCESS_CONTROL_ALLOW_ORIGIN), method);
      assertNull(result.header(ACCESS_CONTROL_ALLOW_CREDENTIALS), method);
    }
  }

  @Test
  void testWildcardInListAllowsAllOrigins() {
    var result = get(ALLOWED_ORIGIN + ", *", true, "https://evil.com");
    assertEquals("*", result.header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertNull(result.header(ACCESS_CONTROL_ALLOW_CREDENTIALS));
  }

  @Test
  void testExplicitOriginIsEchoed() {
    var result = get(ALLOWED_ORIGIN, false, ALLOWED_ORIGIN);
    assertTrue(result.chained);
    assertEquals(ALLOWED_ORIGIN, result.header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertEquals(ORIGIN, result.header(VARY));
    assertNull(result.header(ACCESS_CONTROL_ALLOW_CREDENTIALS));
    assertEquals("X-CSRF-Token", result.header(ACCESS_CONTROL_EXPOSE_HEADERS));
  }

  @Test
  void testExplicitOriginWithCredentials() {
    var result = get(ALLOWED_ORIGIN, true, ALLOWED_ORIGIN);
    assertEquals(ALLOWED_ORIGIN, result.header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertEquals("true", result.header(ACCESS_CONTROL_ALLOW_CREDENTIALS));
  }

  @Test
  void testCredentialsDefaultToFalse() {
    var result = get(filter(ALLOWED_ORIGIN), ALLOWED_ORIGIN);
    assertEquals(ALLOWED_ORIGIN, result.header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertNull(result.header(ACCESS_CONTROL_ALLOW_CREDENTIALS));
  }

  @Test
  void testPreflight() {
    var result = request(filter(ALLOWED_ORIGIN, true), HttpMethod.OPTIONS, ALLOWED_ORIGIN);
    assertFalse(result.chained);
    assertEquals(HttpServletResponse.SC_OK, result.status);
    assertEquals(ALLOWED_ORIGIN, result.header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertEquals("true", result.header(ACCESS_CONTROL_ALLOW_CREDENTIALS));
    assertEquals(ORIGIN, result.header(VARY));
    assertEquals("GET,PUT,POST,DELETE,HEAD,OPTIONS", result.header(ACCESS_CONTROL_ALLOW_METHODS));
    assertEquals("1728000", result.header(ACCESS_CONTROL_MAX_AGE));
  }

  @Test
  void testOriginList() {
    var filter = filter(" https://a.example.com , https://b.example.com:8443 ", false);
    assertEquals(
        "https://a.example.com",
        get(filter, "https://a.example.com").header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertEquals(
        "https://b.example.com:8443",
        get(filter, "https://b.example.com:8443").header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertForbidden(get(filter, "https://b.example.com"));
    assertForbidden(get(filter, "http://a.example.com"));
  }

  @Test
  void testOriginIsNotARegex() {
    var filter = filter(ALLOWED_ORIGIN, true);
    assertForbidden(get(filter, "https://app-example.com"));
    assertForbidden(get(filter, "https://appxexample.com"));
    assertForbidden(get(filter, "https://app.example.com.evil.com"));
    assertForbidden(get(filter, "https://evil.com/https://app.example.com"));
  }

  @Test
  void testRegexOriginIsNotEvaluatedAsRegex() {
    var filter = filter("https://.*\\.example\\.com, https://(a|b)\\.example\\.com", true);
    assertForbidden(get(filter, "https://a.example.com"));
  }

  @Test
  void testInvalidOriginsAreIgnored() {
    var filter = filter("app.example.com, https://*.example.com, " + ALLOWED_ORIGIN, false);
    assertEquals(ALLOWED_ORIGIN, get(filter, ALLOWED_ORIGIN).header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertForbidden(get(filter, "https://x.example.com"));
  }

  @Test
  void testOnlyInvalidOriginsRejectsCrossOrigin() {
    var filter = filter("https://.*\\.example\\.com", false);
    assertForbidden(get(filter, "https://a.example.com"));
    assertTrue(get(filter, "https://" + SERVER_HOST).chained);
  }

  @Test
  void testBlankEntriesAreIgnored() {
    var filter = filter(" , " + ALLOWED_ORIGIN + ",", false);
    assertEquals(ALLOWED_ORIGIN, get(filter, ALLOWED_ORIGIN).header(ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void testForbiddenVariesOnOrigin() {
    assertEquals(ORIGIN, get(ALLOWED_ORIGIN, false, "https://evil.com").header(VARY));
  }

  @Test
  void testConfiguredOriginsAreNormalized() {
    var filter = filter("HTTPS://App.Example.COM/", false);
    assertEquals(ALLOWED_ORIGIN, get(filter, ALLOWED_ORIGIN).header(ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void testDefaultPortsAreStripped() {
    var filter = filter("https://a.example.com:443, http://b.example.com:80", false);
    assertEquals(
        "https://a.example.com",
        get(filter, "https://a.example.com").header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertEquals(
        "http://b.example.com",
        get(filter, "http://b.example.com").header(ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void testNonDefaultPortsAreKept() {
    var filter = filter("https://a.example.com:80, http://b.example.com:443", false);
    assertEquals(
        "https://a.example.com:80",
        get(filter, "https://a.example.com:80").header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertEquals(
        "http://b.example.com:443",
        get(filter, "http://b.example.com:443").header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertForbidden(get(filter, "https://a.example.com"));
    assertForbidden(get(filter, "http://b.example.com"));
  }

  @Test
  void testNullOriginWithWildcard() {
    var result = get("*", true, "null");
    assertTrue(result.chained);
    assertEquals("*", result.header(ACCESS_CONTROL_ALLOW_ORIGIN));
    assertNull(result.header(ACCESS_CONTROL_ALLOW_CREDENTIALS));
  }

  @Test
  void testNullOriginIsNeverAllowedExplicitly() {
    assertForbidden(get("null, " + ALLOWED_ORIGIN, true, "null"));
  }

  private static void assertForbidden(Result result) {
    assertFalse(result.chained);
    assertEquals(HttpServletResponse.SC_FORBIDDEN, result.status);
    assertNull(result.header(ACCESS_CONTROL_ALLOW_ORIGIN));
  }
}
