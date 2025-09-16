/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestUriBuilder {

  @Test
  void buildEmpty() throws URISyntaxException {
    URI expected = new URI("https://example.com/foo?bar#baz");

    URI uri =
        UriBuilder.empty()
            .setScheme("https")
            .setHost("example.com")
            .addPath("foo")
            .addQueryParams("bar")
            .setFragment("baz")
            .toUri();
    assertEquals(expected, uri);

    URI uri2 = UriBuilder.of("https", null, "example.com", null, "foo", "bar", "baz").toUri();
    assertEquals(expected, uri2);
  }

  @Test
  void fromUri() throws URISyntaxException {
    assertEquals(
        new URI("https://example.com/foo?bar#baz"),
        UriBuilder.from("https://example.com/foo?bar#baz").toUri());
  }

  @Test
  void fromRelative() throws URISyntaxException {
    assertEquals(new URI("/vi/foo?bar#baz"), UriBuilder.from("vi/foo?bar#baz").toUri());
    assertEquals(new URI("/vi/foo?bar#baz"), UriBuilder.from("/vi/foo?bar#baz").toUri());
  }

  @Test
  void withPort() throws URISyntaxException {
    assertEquals(
        new URI("https://example.com/foo?bar#baz"),
        UriBuilder.from("https://example.com/foo?bar#baz").setPort(null).toUri());

    URI expected = new URI("https://example.com:443/foo?bar#baz");
    assertEquals(expected, UriBuilder.from("https://example.com:443/foo?bar#baz").toUri());
    assertEquals(
        expected, UriBuilder.from("https://example.com/foo?bar#baz").setPort("443").toUri());
  }

  @Test
  void withMultiplePath() throws URISyntaxException {
    URI expected = new URI("https://example.com/foo/bar/some#baz");

    URI uri =
        UriBuilder.from("https://example.com")
            .addPath("foo")
            .addPath("/bar")
            .addPath("/some/")
            .setFragment("baz")
            .toUri();
    assertEquals(expected, uri);

    URI uri2 =
        UriBuilder.from("https://example.com/foo#baz").addPath("/bar").addPath("/some/").toUri();
    assertEquals(expected, uri2);
  }

  @Test
  void withQueryParam() throws URISyntaxException {
    URI expected = new URI("https://example.com/some?q1=test1&q2=test2");

    URI uri = UriBuilder.from("https://example.com/some?q1=test1&q2=test2").toUri();
    assertEquals(expected, uri);

    URI uri2 =
        UriBuilder.from("https://example.com/some?q1=test1").addQueryParam("q2", "test2").toUri();
    assertEquals(expected, uri2);

    URI uri3 =
        UriBuilder.from("https://example.com/some?q1=test1").addQueryParams("q2=test2").toUri();
    assertEquals(expected, uri3);

    URI uri4 =
        UriBuilder.from("https://example.com/some?q1=test1")
            .addQueryParams(Map.of("q2", "test2"))
            .toUri();
    assertEquals(expected, uri4);
  }

  @Test
  void merge() throws URISyntaxException {
    UriBuilder base = UriBuilder.from("https://example.com/v1");

    assertEquals(
        new URI("https://example.com/v1/foo?bar#baz"),
        base.cloneBuilder().merge(UriBuilder.from("foo?bar#baz")).toUri());
    assertEquals(
        new URI("https://example.com/v1/foo?bar#baz"),
        base.cloneBuilder().merge(UriBuilder.from("/foo?bar#baz")).toUri());

    // Weird
    assertEquals(
        new URI("https://example.com/foo/v1?bar#baz"),
        UriBuilder.from("/foo?bar#baz").merge(base.cloneBuilder()).toUri());
    assertEquals(
        new URI("https://test.com:8080/v1/sub?foo#foz"),
        base.cloneBuilder().merge(UriBuilder.from("https://test.com:8080/sub?foo#foz")).toUri());
  }
}
