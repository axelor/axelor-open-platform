/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

  static final String text1 =
      "" + "  this is some text\n" + "  this is some text\n" + "  this is some text\n";

  static final String text2 =
      "" + "  this is some text\n" + "    \tthis is some text\n" + "   this is some text\n";

  static final String text3 =
      "" + "  this is some text\n" + "  this is some text\n" + " this is some text\n";

  static final String text4 =
      "" + "this is some text\n" + "    |this is some text\n" + "    |this is some text\n";

  @Test
  public void testStripIndent() {
    String[] lines = StringUtils.stripIndent(text1).split("\n");
    assertFalse(Character.isWhitespace(lines[0].charAt(0)));
    assertFalse(Character.isWhitespace(lines[1].charAt(0)));
    assertFalse(Character.isWhitespace(lines[2].charAt(0)));
    assertEquals(
        Joiner.on("\n").join(lines),
        "" + "this is some text\n" + "this is some text\n" + "this is some text");

    lines = StringUtils.stripIndent(text2).split("\n");
    assertFalse(Character.isWhitespace(lines[0].charAt(0)));
    assertTrue(Character.isWhitespace(lines[1].charAt(0)));
    assertTrue(Character.isWhitespace(lines[2].charAt(0)));
    assertEquals(
        Joiner.on("\n").join(lines),
        "" + "this is some text\n" + "  \tthis is some text\n" + " this is some text");

    lines = StringUtils.stripIndent(text3).split("\n");
    assertTrue(Character.isWhitespace(lines[0].charAt(0)));
    assertTrue(Character.isWhitespace(lines[1].charAt(0)));
    assertFalse(Character.isWhitespace(lines[2].charAt(0)));
    assertEquals(
        Joiner.on("\n").join(lines),
        "" + " this is some text\n" + " this is some text\n" + "this is some text");
  }

  @Test
  public void testStripMargin() {
    String[] lines = StringUtils.stripMargin(text4).split("\n");
    assertEquals(
        Joiner.on("\n").join(lines),
        "" + "this is some text\n" + "this is some text\n" + "this is some text");
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
