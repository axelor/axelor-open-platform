/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

public abstract class AbstractMapTester {

  void assertMap(Map<String, String> map) {
    assertEquals("James", map.get("test"));
    assertNull(map.get("name.sub"));
    assertEquals("Foo", map.get("name.sub.my-foo"));
    assertEquals("Bar", map.get("name.sub.my-bar"));
    assertNull(map.get("ignore"));
    assertEquals("one", map.get("list[0]"));
    assertEquals("two", map.get("list[1]"));
  }
}
