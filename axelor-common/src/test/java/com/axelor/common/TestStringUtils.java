/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Joiner;
import org.junit.jupiter.api.Test;

public class TestStringUtils {

  @Test
  public void testIsEmpty() {
    assertTrue(StringUtils.isEmpty(null));
    assertTrue(StringUtils.isEmpty(""));
    assertFalse(StringUtils.isEmpty(" "));
  }

  @Test
  public void testIsBlank() {
    assertTrue(StringUtils.isBlank(null));
    assertTrue(StringUtils.isBlank(""));
    assertTrue(StringUtils.isBlank(" "));
    assertFalse(StringUtils.isBlank("some value"));
  }

  @Test
  public void testEmptyToNull() {
    assertNull(StringUtils.emptyToNull(null));
    assertNull(StringUtils.emptyToNull(""));
    assertEquals(" ", StringUtils.emptyToNull(" "));
    assertEquals("some value", StringUtils.emptyToNull("some value"));
  }

  @Test
  public void testTruncate() {
    assertNull(StringUtils.truncate(null, 5));
    assertEquals("", StringUtils.truncate("", 5));
    assertEquals("", StringUtils.truncate("some value", 0));
    assertEquals("some", StringUtils.truncate("some value", 4));
    assertEquals("some value", StringUtils.truncate("some value", 10));
    assertEquals("some value", StringUtils.truncate("some value", 20));

    // surrogate pair is not split
    assertEquals("ab", StringUtils.truncate("ab😀cd", 3));
    assertEquals("ab😀", StringUtils.truncate("ab😀cd", 4));

    assertThrows(IllegalArgumentException.class, () -> StringUtils.truncate("some value", -1));
  }

  static final String text1 =
      """
        this is some text
        this is some text
        this is some text
      """;

  static final String text2 =
      """
        this is some text
          	this is some text
         this is some text
      """;

  static final String text3 =
      """
        this is some text
        this is some text
       this is some text
      """;

  static final String text4 =
      """
      this is some text
          |this is some text
          |this is some text
      """;

  @Test
  public void testStripIndent() {
    String[] lines = StringUtils.stripIndent(text1).split("\n");
    assertFalse(Character.isWhitespace(lines[0].charAt(0)));
    assertFalse(Character.isWhitespace(lines[1].charAt(0)));
    assertFalse(Character.isWhitespace(lines[2].charAt(0)));
    assertEquals(
        Joiner.on("\n").join(lines),
        """
        this is some text
        this is some text
        this is some text""");

    lines = StringUtils.stripIndent(text2).split("\n");
    assertFalse(Character.isWhitespace(lines[0].charAt(0)));
    assertTrue(Character.isWhitespace(lines[1].charAt(0)));
    assertTrue(Character.isWhitespace(lines[2].charAt(0)));
    assertEquals(
        Joiner.on("\n").join(lines),
        """
        this is some text
          	this is some text
         this is some text""");

    lines = StringUtils.stripIndent(text3).split("\n");
    assertTrue(Character.isWhitespace(lines[0].charAt(0)));
    assertTrue(Character.isWhitespace(lines[1].charAt(0)));
    assertFalse(Character.isWhitespace(lines[2].charAt(0)));
    assertEquals(
        Joiner.on("\n").join(lines),
        """
         this is some text
         this is some text
        this is some text""");
  }

  @Test
  public void testStripMargin() {
    String[] lines = StringUtils.stripMargin(text4).split("\n");
    assertEquals(
        Joiner.on("\n").join(lines),
        """
        this is some text
        this is some text
        this is some text""");
  }

  @Test
  public void testStripAccent() {
    assertEquals(
        "AAAAAACEEEEIIIINOOOOOUUUUY", StringUtils.stripAccent("ÀÁÂÃÄÅÇÈÉÊËÌÍÎÏÑÒÓÔÕÖÙÚÛÜÝ"));
    assertEquals(
        "aaaaaaceeeeiiiinooooouuuuyy", StringUtils.stripAccent("àáâãäåçèéêëìíîïñòóôõöùúûüýÿ"));
    assertEquals("L", StringUtils.stripAccent("Ł"));
    assertEquals("l", StringUtils.stripAccent("ł"));
    assertEquals("Cesar", StringUtils.stripAccent("César"));
    assertEquals("Andre", StringUtils.stripAccent("André"));
  }

  @Test
  public void testAsciiChar() {
    assertTrue(StringUtils.isAscii(' '));
    assertTrue(StringUtils.isAscii('a'));
    assertTrue(StringUtils.isAscii('A'));
    assertTrue(StringUtils.isAscii('3'));
    assertTrue(StringUtils.isAscii('-'));
    assertFalse(StringUtils.isAscii('\n'));
    assertFalse(StringUtils.isAscii('é'));
  }

  @Test
  public void testAsciiText() {
    assertFalse(StringUtils.isAscii(null));
    assertTrue(StringUtils.isAscii(""));
    assertTrue(StringUtils.isAscii(" "));
    assertTrue(StringUtils.isAscii("Toto"));
    assertTrue(StringUtils.isAscii("ab12"));
    assertTrue(StringUtils.isAscii("!ab-c~"));
    assertTrue(StringUtils.isAscii("\u0020")); // space
    assertFalse(StringUtils.isAscii("\u00FC")); // ü
    assertFalse(StringUtils.isAscii("é"));
  }

  @Test
  public void testSplitToArray() {
    assertEquals(0, StringUtils.splitToArray(null, ",").length);
    assertEquals(0, StringUtils.splitToArray("", ",").length);
    assertEquals(0, StringUtils.splitToArray(" ", ",").length);

    assertArrayEquals(new String[] {"foo"}, StringUtils.splitToArray("foo", ","));
    assertArrayEquals(new String[] {"foo", "bar"}, StringUtils.splitToArray("foo,bar", ","));
    assertArrayEquals(new String[] {"foo", "bar"}, StringUtils.splitToArray("foo,,bar", ","));
    assertArrayEquals(new String[] {"foo", "bar"}, StringUtils.splitToArray("foo , , bar", ","));

    assertArrayEquals(
        new String[] {"foo ", " ", " bar"},
        StringUtils.splitToArray("foo , , bar", ",", false, false));
  }
}
