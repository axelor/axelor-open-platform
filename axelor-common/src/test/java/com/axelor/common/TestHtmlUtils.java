/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class TestHtmlUtils {

  @Test
  public void testEscapeAttribute() {
    assertNull(HtmlUtils.escapeAttribute(null));
    assertEquals("", HtmlUtils.escapeAttribute(""));
    assertEquals(" ", HtmlUtils.escapeAttribute(" "));
    assertEquals("&gt;", HtmlUtils.escapeAttribute(">"));
    assertEquals("&lt;", HtmlUtils.escapeAttribute("<"));

    assertEquals(
        "ho&#39; : &quot;a nice quote&quot;", HtmlUtils.escapeAttribute("ho' : \"a nice quote\""));
    assertEquals("&lt;script&gt;", HtmlUtils.escapeAttribute("<script>"));
  }

  @Test
  public void testEscape() {
    assertNull(HtmlUtils.escape(null));
    assertEquals("", HtmlUtils.escapeAttribute(""));
    assertEquals(" ", HtmlUtils.escapeAttribute(" "));
    assertEquals("&gt;", HtmlUtils.escape(">"));
    assertEquals("&lt;", HtmlUtils.escape("<"));

    assertEquals(
        "ho&#39; : &quot;a nice quote&quot;", HtmlUtils.escapeAttribute("ho' : \"a nice quote\""));
    assertEquals("&lt;script&gt;", HtmlUtils.escape("<script>"));
  }
}
