/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
