/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class TestObjectUtils {

  @Test
  public void testIsEmpty() {
    assertTrue(ObjectUtils.isEmpty(null));
    assertTrue(ObjectUtils.isEmpty(""));
    assertFalse(ObjectUtils.isEmpty(" "));
    assertTrue(ObjectUtils.isEmpty(Optional.empty()));
    assertFalse(ObjectUtils.isEmpty(Optional.of("some")));
    assertTrue(ObjectUtils.isEmpty(new String[] {}));
    assertFalse(ObjectUtils.isEmpty(new String[] {"some", "value"}));
    assertTrue(ObjectUtils.isEmpty(new HashMap<>()));
    assertFalse(ObjectUtils.isEmpty(ImmutableMap.of("some", "value")));
    assertTrue(ObjectUtils.isEmpty(Arrays.asList()));
    assertFalse(ObjectUtils.isEmpty(Arrays.asList("some", "value")));
  }

  @Test
  public void testGetResource() {
    URL url = ResourceUtils.getResource("test.txt");
    assertNotNull(url);
  }

  @Test
  public void testGetResourceStream() {
    InputStream stream = ResourceUtils.getResourceStream("test.txt");
    assertNotNull(stream);
  }
}
