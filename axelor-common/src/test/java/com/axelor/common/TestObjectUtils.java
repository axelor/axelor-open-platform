/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
    assertFalse(ObjectUtils.isEmpty(Map.of("some", "value")));
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
