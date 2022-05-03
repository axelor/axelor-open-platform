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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;

public class TestPropertiesUtils extends AbstractMapTester {

  @Test
  public void wrongFileTest() throws IOException {
    Properties prop = PropertiesUtils.loadProperties(ResourceUtils.getResource("wrong.properties"));
    assertTrue(prop.isEmpty());
  }

  @Test
  public void loadProperties() throws IOException {
    Properties prop = PropertiesUtils.loadProperties(ResourceUtils.getResource("test.properties"));
    assertEquals(5, prop.size());
  }

  @Test
  public void toMapTest() throws IOException {
    Properties prop = PropertiesUtils.loadProperties(ResourceUtils.getResource("test.properties"));
    assertMap(PropertiesUtils.propertiesToMap(prop));
  }
}
