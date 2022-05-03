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
package com.axelor.meta.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

public class TestModuleResolver {

  private Map<String, Properties> modules = new LinkedHashMap<>();

  private void add(String name, String... depends) {
    Properties props = new Properties();
    props.setProperty("name", name);
    props.setProperty("depends", String.join(",", depends));
    modules.put(name, props);
  }

  @Test
  public void test() {

    add("axelor-auth", "axelor-core");
    add("axelor-meta", "axelor-data");

    add("axelor-x");

    add("axelor-sale", "axelor-contact");
    add("axelor-data", "axelor-auth", "axelor-core");
    add("axelor-contact", "axelor-auth", "axelor-core", "axelor-meta");
    add("axelor-project", "axelor-sale");

    List<String> expected =
        Lists.newArrayList(
            "axelor-core",
            "axelor-auth",
            "axelor-data",
            "axelor-meta",
            "axelor-contact",
            "axelor-sale");

    ModuleResolver resolver = new ModuleResolver(modules.values());

    List<String> actual = new ArrayList<>();
    for (Module module : resolver.resolve("axelor-sale")) {
      actual.add(module.getName());
    }

    assertEquals(expected, actual);

    List<String> all = resolver.names();

    assertEquals("axelor-core", all.get(0));
    assertEquals("axelor-project", all.get(all.size() - 1));
  }
}
