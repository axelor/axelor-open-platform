/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        List.of(
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

    assertEquals("axelor-core", all.getFirst());
    assertEquals("axelor-project", all.getLast());
  }
}
