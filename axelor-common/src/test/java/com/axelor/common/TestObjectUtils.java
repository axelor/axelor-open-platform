/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

import com.google.common.collect.ImmutableMap;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

public class TestObjectUtils {

  @Test
  public void testIsEmpty() {
    Assert.assertTrue(ObjectUtils.isEmpty(null));
    Assert.assertTrue(ObjectUtils.isEmpty(""));
    Assert.assertFalse(ObjectUtils.isEmpty(" "));
    Assert.assertTrue(ObjectUtils.isEmpty(Optional.empty()));
    Assert.assertFalse(ObjectUtils.isEmpty(Optional.of("some")));
    Assert.assertTrue(ObjectUtils.isEmpty(new String[] {}));
    Assert.assertFalse(ObjectUtils.isEmpty(new String[] {"some", "value"}));
    Assert.assertTrue(ObjectUtils.isEmpty(new HashMap<>()));
    Assert.assertFalse(ObjectUtils.isEmpty(ImmutableMap.of("some", "value")));
    Assert.assertTrue(ObjectUtils.isEmpty(Arrays.asList()));
    Assert.assertFalse(ObjectUtils.isEmpty(Arrays.asList("some", "value")));
  }

  @Test
  public void testGetResource() {
    URL url = ResourceUtils.getResource("axelor-version.txt");
    Assert.assertNotNull(url);
  }

  @Test
  public void testGetResourceStream() {
    InputStream stream = ResourceUtils.getResourceStream("axelor-version.txt");
    Assert.assertNotNull(stream);
  }
}
