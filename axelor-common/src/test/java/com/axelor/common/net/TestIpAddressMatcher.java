/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TestIpAddressMatcher {

  @Test
  void testIPv4Address() {
    var matcher = IpAddressMatcher.of(" 192.0.2.10 ");
    assertEquals("192.0.2.10", matcher.toString());
    assertTrue(matcher.matches("192.0.2.10"));
    assertFalse(matcher.matches("192.0.2.11"));
  }

  @Test
  void testIPv4Range() {
    var matcher = IpAddressMatcher.of("10.0.0.0/8");
    assertEquals("10.0.0.0/8", matcher.toString());
    assertTrue(matcher.matches("10.0.0.0"));
    assertTrue(matcher.matches("10.255.255.255"));
    assertFalse(matcher.matches("11.0.0.0"));
    assertFalse(matcher.matches("9.255.255.255"));
  }

  @Test
  void testNonOctetPrefix() {
    var matcher = IpAddressMatcher.of("172.16.0.0/12");
    assertTrue(matcher.matches("172.31.255.255"));
    assertFalse(matcher.matches("172.32.0.0"));
  }

  @Test
  void testHostBitsAreCleared() {
    var matcher = IpAddressMatcher.of("192.168.1.10/24");
    assertEquals("192.168.1.0/24", matcher.toString());
    assertTrue(matcher.matches("192.168.1.200"));
    assertFalse(matcher.matches("192.168.2.10"));
  }

  @Test
  void testZeroPrefix() {
    assertTrue(IpAddressMatcher.of("0.0.0.0/0").matches("203.0.113.1"));
    assertFalse(IpAddressMatcher.of("0.0.0.0/0").matches("2001:db8::1"));
    assertTrue(IpAddressMatcher.of("::/0").matches("2001:db8::1"));
  }

  @Test
  void testIPv6() {
    var matcher = IpAddressMatcher.of("2001:0DB8:0000::/32");
    assertEquals("2001:db8::/32", matcher.toString());
    assertTrue(matcher.matches("2001:db8:ffff::1"));
    assertFalse(matcher.matches("2001:db9::1"));

    assertEquals("2001:db8::1", IpAddressMatcher.of("[2001:db8:0:0::1]").toString());
  }

  @Test
  void testIPv6ZoneIndex() {
    assertTrue(IpAddressMatcher.of("fe80::/10").matches("fe80::1%eth0"));
  }

  @Test
  void testIPv4MappedIPv6() {
    assertTrue(IpAddressMatcher.of("192.0.2.0/24").matches("::ffff:192.0.2.5"));
    assertEquals("192.0.2.5", IpAddressMatcher.of("::ffff:192.0.2.5").toString());
    assertTrue(IpAddressMatcher.of("::ffff:192.0.2.5").matches("192.0.2.5"));
  }

  @Test
  void testNoCrossFamilyMatch() {
    assertFalse(IpAddressMatcher.of("::1").matches("127.0.0.1"));
    assertFalse(IpAddressMatcher.of("127.0.0.1").matches("::1"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        " ",
        "localhost",
        "example.com",
        "300.1.1.1",
        "1.2.3",
        "1.2.3.4:80",
        "10.0.0.0/",
        "10.0.0.0/33",
        "10.0.0.0/-1",
        "10.0.0.0/+8",
        "10.0.0.0/8/8",
        "2001:db8::/129",
        "2001:db8::1/abc"
      })
  void testInvalid(String value) {
    assertThrows(IllegalArgumentException.class, () -> IpAddressMatcher.of(value));
  }

  @Test
  void testMatchesInvalidAddress() {
    var matcher = IpAddressMatcher.of("0.0.0.0/0");
    assertFalse(matcher.matches("localhost"));
    assertFalse(matcher.matches(""));
    assertFalse(matcher.matches((String) null));
    assertNull(IpAddressMatcher.parseAddress("example.com"));
  }

  @Test
  void testParseList() {
    var matchers = IpAddressMatcher.parseList(" 10.0.0.1, 192.168.0.0/16 ,2001:db8::/32,, ");
    assertEquals(
        List.of("10.0.0.1", "192.168.0.0/16", "2001:db8::/32"),
        matchers.stream().map(Object::toString).toList());

    assertTrue(IpAddressMatcher.parseList(null).isEmpty());
    assertTrue(IpAddressMatcher.parseList(" , ").isEmpty());
    assertThrows(IllegalArgumentException.class, () -> IpAddressMatcher.parseList("10.0.0.1, foo"));
    // entries must be separated by commas
    assertThrows(
        IllegalArgumentException.class, () -> IpAddressMatcher.parseList("10.0.0.1 10.0.0.2"));
    assertThrows(
        IllegalArgumentException.class, () -> IpAddressMatcher.parseList("10.0.0.1\n10.0.0.2"));
  }

  @Test
  void testMatchesAny() {
    var matchers = IpAddressMatcher.parseList("10.0.0.1, 2001:db8::/32");
    assertTrue(IpAddressMatcher.matchesAny(matchers, "10.0.0.1"));
    assertTrue(IpAddressMatcher.matchesAny(matchers, "2001:db8::42"));
    assertFalse(IpAddressMatcher.matchesAny(matchers, "10.0.0.2"));
    assertFalse(IpAddressMatcher.matchesAny(matchers, null));
    assertFalse(IpAddressMatcher.matchesAny(List.of(), "10.0.0.1"));
  }
}
