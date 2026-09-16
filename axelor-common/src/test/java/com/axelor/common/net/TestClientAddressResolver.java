/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.net;

import static com.axelor.common.net.ClientAddressResolver.X_FORWARDED_FOR;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TestClientAddressResolver {

  private static final String PEER = "10.0.0.5";
  private static final String CLIENT = "198.51.100.7";
  private static final String OTHER = "203.0.113.10";
  private static final String X_REAL_IP = "X-Real-IP";
  private static final String CF_CONNECTING_IP = "CF-Connecting-IP";

  private final ClientAddressResolver resolver = new ClientAddressResolver(null);

  private static String resolve(ClientAddressResolver resolver, Map<String, String> headers) {
    return resolver.resolve(headers::get, PEER);
  }

  @Test
  void testNoHeader() {
    assertEquals(PEER, resolve(resolver, Map.of()));
  }

  @Test
  void testForwardedForLeftMost() {
    assertEquals(CLIENT, resolve(resolver, Map.of(X_FORWARDED_FOR, CLIENT)));
    assertEquals(CLIENT, resolve(resolver, Map.of(X_FORWARDED_FOR, " " + CLIENT + " , " + OTHER)));
  }

  @Test
  void testNoFallbackToOtherHeaders() {
    // other headers may not be overwritten by the reverse proxy, so may be forged
    assertEquals(PEER, resolve(resolver, Map.of(X_REAL_IP, CLIENT)));
    assertEquals(PEER, resolve(resolver, Map.of(X_FORWARDED_FOR, " ", X_REAL_IP, CLIENT)));
    assertEquals(CLIENT, resolve(resolver, Map.of(X_FORWARDED_FOR, CLIENT, X_REAL_IP, OTHER)));
  }

  @Test
  void testInvalidHeaderUsesPeer() {
    assertEquals(PEER, resolve(resolver, Map.of(X_FORWARDED_FOR, "unknown", X_REAL_IP, CLIENT)));
    assertEquals(PEER, resolve(resolver, Map.of(X_FORWARDED_FOR, ", " + CLIENT)));
    assertEquals(PEER, resolve(resolver, Map.of(X_FORWARDED_FOR, "example.com")));
  }

  @Test
  void testAddressesWithPort() {
    assertEquals(CLIENT, resolve(resolver, Map.of(X_FORWARDED_FOR, CLIENT + ":1234")));
    assertEquals("2001:db8::1", resolve(resolver, Map.of(X_FORWARDED_FOR, "[2001:db8::1]:443")));
    assertEquals("2001:db8::1", resolve(resolver, Map.of(X_FORWARDED_FOR, "[2001:db8::1]")));
    assertEquals(PEER, resolve(resolver, Map.of(X_FORWARDED_FOR, CLIENT + ":http")));
  }

  @Test
  void testNormalizedAddress() {
    assertEquals(CLIENT, resolve(resolver, Map.of(X_FORWARDED_FOR, "::ffff:" + CLIENT)));
    assertEquals("2001:db8::1", resolve(resolver, Map.of(X_FORWARDED_FOR, "2001:DB8:0::1")));
  }

  @Test
  void testCustomHeaderOnly() {
    var customResolver = new ClientAddressResolver(" " + CF_CONNECTING_IP + " ");

    assertEquals(
        CLIENT,
        resolve(
            customResolver,
            Map.of(CF_CONNECTING_IP, CLIENT, X_FORWARDED_FOR, OTHER, X_REAL_IP, OTHER)));
    assertEquals(CLIENT, resolve(customResolver, Map.of(CF_CONNECTING_IP, CLIENT + ", " + OTHER)));
  }

  @Test
  void testCustomHeaderMissingOrInvalidUsesPeer() {
    var customResolver = new ClientAddressResolver(CF_CONNECTING_IP);

    assertEquals(PEER, resolve(customResolver, Map.of()));
    assertEquals(PEER, resolve(customResolver, Map.of(X_FORWARDED_FOR, CLIENT, X_REAL_IP, CLIENT)));
    assertEquals(PEER, resolve(customResolver, Map.of(CF_CONNECTING_IP, "")));
    assertEquals(PEER, resolve(customResolver, Map.of(CF_CONNECTING_IP, "unknown")));
  }

  @Test
  void testRealIpHeader() {
    var customResolver = new ClientAddressResolver(X_REAL_IP);

    assertEquals(
        CLIENT, resolve(customResolver, Map.of(X_REAL_IP, CLIENT, X_FORWARDED_FOR, OTHER)));
    assertEquals(PEER, resolve(customResolver, Map.of(X_FORWARDED_FOR, CLIENT)));
  }

  @Test
  void testBlankCustomHeaderUsesDefault() {
    var blankResolver = new ClientAddressResolver(" ");

    assertEquals(CLIENT, resolve(blankResolver, Map.of(X_FORWARDED_FOR, CLIENT)));
    assertEquals(PEER, resolve(blankResolver, Map.of(X_REAL_IP, CLIENT)));
  }
}
